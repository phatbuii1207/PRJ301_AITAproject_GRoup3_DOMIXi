package com.fpt.aita.tools;

import com.fpt.aita.config.AppConfig;
import com.fpt.aita.dal.DBContext;
import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** One-time, read-only SQL Server import. Refuses a nonempty target and verifies every row. */
public final class MigrateSqlServer {
    private static final String[] TABLES = {"CLASSES","USERS","USER_CLASS","ASSIGNMENTS","SUBMISSIONS","SOURCE_FILES",
            "METHODS","RULES","ISSUES","ANALYSIS_REPORTS","AI_OPTIMIZATIONS","GRADING_CONFIGS","GRADING_TEST_CASES","GRADING_REPORTS","GRADING_CASE_RESULTS"};
    private static final Map<String,Object> REPORT = new LinkedHashMap<>();
    private static Map<String,String> sourceValues;
    private static String source(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) value = sourceValues.get(key);
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("Missing source configuration: " + key);
        return value;
    }
    private static String pg(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
    private static String ms(String identifier) { return "[" + identifier.replace("]", "]]") + "]"; }
    public static void main(String[] args) {
        try {
            String config = System.getenv("AITA_MIGRATION_SOURCE_FILE");
            sourceValues = AppConfig.read(Paths.get(config == null ? ".git/aita-postgres-source.properties" : config));
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection original = DriverManager.getConnection(source("SOURCE_DB_URL"), source("SOURCE_DB_USER"), source("SOURCE_DB_PASSWORD"));
                 Connection target = DBContext.openConnection()) {
                original.setReadOnly(true);
                target.setAutoCommit(false);
                try {
                    verifyTableSet(original);
                    // Preflight all attributes/keys and emptiness before writing any data.
                    for (String table : TABLES) { verifySchema(original, target, table); requireEmpty(target, table); }
                    for (String table : TABLES) copy(original, target, table);
                    target.commit(); REPORT.put("result", "PASS");
                } catch (Exception ex) { target.rollback(); throw ex; }
            }
            Files.createDirectories(Paths.get("target"));
            Files.write(Paths.get("target/postgresql-migration-verification.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(REPORT).getBytes(StandardCharsets.UTF_8));
            System.out.println("Migration PASS: table/column/key metadata, row counts and content hashes match; identities preserved.");
        } catch (Exception ex) {
            // Do not print JDBC URLs, credentials, statements or row values on a failed migration.
            System.err.println("Migration aborted: " + ex.getClass().getSimpleName()
                    + (ex instanceof IllegalStateException ? " - " + ex.getMessage() : " (check connection/schema; no source data was changed)"));
            System.exit(1);
        }
    }
    private static void verifyTableSet(Connection source) throws SQLException {
        Set<String> actual = new TreeSet<>();
        try (ResultSet rs = source.getMetaData().getTables(source.getCatalog(), "dbo", "%", new String[]{"TABLE"})) {
            while (rs.next()) if (!"sysdiagrams".equalsIgnoreCase(rs.getString("TABLE_NAME"))) actual.add(rs.getString("TABLE_NAME"));
        }
        if (!actual.equals(new TreeSet<>(Arrays.asList(TABLES)))) throw new IllegalStateException("Unexpected source table set; preserve additional tables before importing");
    }
    private static List<Map<String,Object>> columns(Connection db, String schema, String table) throws SQLException {
        List<Map<String,Object>> result = new ArrayList<>();
        try (ResultSet rs = db.getMetaData().getColumns(db.getCatalog(), schema, table, null)) {
            while (rs.next()) {
                Map<String,Object> column = new LinkedHashMap<>();
                column.put("name", rs.getString("COLUMN_NAME")); column.put("nullable", rs.getInt("NULLABLE"));
                column.put("type", rs.getInt("DATA_TYPE")); column.put("size", rs.getInt("COLUMN_SIZE"));
                column.put("scale", rs.getInt("DECIMAL_DIGITS")); column.put("identity", "YES".equals(rs.getString("IS_AUTOINCREMENT")));
                result.add(column);
            }
        }
        return result;
    }
    private static List<String> keys(Connection db, String schema, String table) throws SQLException {
        List<String> result = new ArrayList<>();
        try (ResultSet rs = db.getMetaData().getPrimaryKeys(db.getCatalog(), schema, table)) {
            while (rs.next()) result.add("PK:" + rs.getInt("KEY_SEQ") + ":" + rs.getString("COLUMN_NAME"));
        }
        try (ResultSet rs = db.getMetaData().getImportedKeys(db.getCatalog(), schema, table)) {
            while (rs.next()) result.add("FK:" + rs.getString("FKCOLUMN_NAME") + ":" + rs.getString("PKTABLE_NAME")
                    + ":" + rs.getString("PKCOLUMN_NAME") + ":" + rs.getInt("DELETE_RULE") + ":" + rs.getInt("UPDATE_RULE"));
        }
        Collections.sort(result); return result;
    }
    private static void verifySchema(Connection source, Connection target, String table) throws SQLException {
        List<Map<String,Object>> a = columns(source, "dbo", table), b = columns(target, "public", table);
        if (a.isEmpty() || a.size() != b.size()) throw new IllegalStateException("Column count mismatch: " + table);
        for (int i=0;i<a.size();i++) {
            Map<String,Object> left=a.get(i), right=b.get(i);
            if (!left.get("name").equals(right.get("name")) || !left.get("nullable").equals(right.get("nullable"))
                    || !left.get("identity").equals(right.get("identity"))) throw new IllegalStateException("Attribute mismatch: " + table + "." + left.get("name"));
            int type=(Integer)left.get("type"), targetType=(Integer)right.get("type");
            boolean string=type==Types.VARCHAR || type==Types.NVARCHAR || type==Types.LONGNVARCHAR || type==Types.LONGVARCHAR;
            boolean compatible=string ? targetType==Types.VARCHAR || targetType==Types.LONGVARCHAR
                    : type==Types.BIT || type==Types.BOOLEAN ? targetType==Types.BIT || targetType==Types.BOOLEAN
                    : type==Types.DECIMAL || type==Types.NUMERIC ? targetType==Types.DECIMAL || targetType==Types.NUMERIC : type==targetType;
            if (!compatible) throw new IllegalStateException("Column type mismatch: " + table + "." + left.get("name"));
            if ((string && (Integer)left.get("size") < 1000000 && !left.get("size").equals(right.get("size")))
                    || ((type==Types.DECIMAL || type==Types.NUMERIC) && (!left.get("size").equals(right.get("size")) || !left.get("scale").equals(right.get("scale"))))) {
                throw new IllegalStateException("Length/precision mismatch: " + table + "." + left.get("name"));
            }
        }
        if (!keys(source,"dbo",table).equals(keys(target,"public",table))) throw new IllegalStateException("Primary/foreign key mismatch: " + table);
    }
    private static void requireEmpty(Connection db, String table) throws SQLException {
        try (Statement s=db.createStatement();ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM public."+pg(table))) {
            rs.next();if(rs.getLong(1)!=0)throw new IllegalStateException("Target is not empty: " + table + "; import into a new database");
        }
    }
    private static List<String> primary(Connection db, String table) throws SQLException {
        SortedMap<Integer,String> result=new TreeMap<>();
        try(ResultSet rs=db.getMetaData().getPrimaryKeys(db.getCatalog(),"dbo",table)) {
            while(rs.next())result.put(rs.getInt("KEY_SEQ"),rs.getString("COLUMN_NAME"));
        }
        return new ArrayList<>(result.values());
    }
    private static String order(List<String> keys,boolean postgres) {
        List<String> parts=new ArrayList<>();for(String k:keys)parts.add(postgres?pg(k):ms(k));return String.join(",",parts);
    }
    private static void copy(Connection source, Connection target, String table) throws Exception {
        List<Map<String,Object>> cols=columns(source,"dbo",table);List<String> names=new ArrayList<>(),params=new ArrayList<>();
        for(Map<String,Object> c:cols){names.add(pg((String)c.get("name")));params.add("?");}
        String insert="INSERT INTO public."+pg(table)+" ("+String.join(",",names)+") VALUES ("+String.join(",",params)+")";
        long count=0;MessageDigest originalHash=MessageDigest.getInstance("SHA-256");List<String> pk=primary(source,table);
        try(Statement s=source.createStatement();ResultSet rs=s.executeQuery("SELECT * FROM dbo."+ms(table)+" ORDER BY "+order(pk,false));PreparedStatement ps=target.prepareStatement(insert)) {
            while(rs.next()) {
                for(int i=1;i<=cols.size();i++) {
                    Object v=rs.getObject(i);hash(originalHash,v);
                    if(v instanceof String)ps.setString(i,(String)v);
                    else if(v instanceof Boolean)ps.setBoolean(i,(Boolean)v);
                    else if(v instanceof Timestamp)ps.setTimestamp(i,(Timestamp)v);
                    else if(v instanceof BigDecimal)ps.setBigDecimal(i,(BigDecimal)v);
                    else ps.setObject(i,v);
                }
                ps.addBatch();if(++count%100==0)ps.executeBatch();
            }
            ps.executeBatch();
        }
        MessageDigest targetHash=MessageDigest.getInstance("SHA-256");long copied=0;
        try(Statement s=target.createStatement();ResultSet rs=s.executeQuery("SELECT * FROM public."+pg(table)+" ORDER BY "+order(pk,true))) {
            while(rs.next()){for(int i=1;i<=cols.size();i++)hash(targetHash,rs.getObject(i));copied++;}
        }
        byte[] first=originalHash.digest(),second=targetHash.digest();
        if(count!=copied || !Arrays.equals(first,second))throw new IllegalStateException("Row content mismatch: "+table);
        for(Map<String,Object> col:cols)if(Boolean.TRUE.equals(col.get("identity")))syncIdentity(source,target,table,(String)col.get("name"));
        Map<String,Object> result=new LinkedHashMap<>();result.put("columns",cols.size());result.put("rows",count);result.put("contentSha256",hex(first));result.put("keysAndAttributesMatch",true);
        REPORT.put(table,result);System.out.println(table+": "+count+" rows; columns/keys/content PASS");
    }
    private static void syncIdentity(Connection source,Connection target,String table,String column)throws SQLException {
        Long last=null;
        try(PreparedStatement ps=source.prepareStatement("SELECT CAST(last_value AS bigint) FROM sys.identity_columns WHERE object_id=OBJECT_ID(?) AND name=?")) {
            ps.setString(1,"dbo."+table);ps.setString(2,column);
            try(ResultSet rs=ps.executeQuery()){if(rs.next()){long value=rs.getLong(1);if(!rs.wasNull())last=value;}}
        }
        try(PreparedStatement ps=target.prepareStatement("SELECT setval(pg_get_serial_sequence(?,?),?,?)")) {
            ps.setString(1,"public."+pg(table));ps.setString(2,column);ps.setLong(3,last==null?1:last);ps.setBoolean(4,last!=null);ps.execute();
        }
    }
    private static void hash(MessageDigest digest,Object value) {
        if(value==null){digest.update((byte)0);return;}
        String text;
        if(value instanceof Timestamp) {
            Timestamp stamp=(Timestamp)value;LocalDateTime rounded=stamp.toLocalDateTime().withNano(0).plusNanos(((stamp.getNanos()+500L)/1000L)*1000L);
            text=rounded.toString();
        }else text=value.toString();
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);digest.update((byte)1);
        digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
    }
    private static String hex(byte[] values){StringBuilder out=new StringBuilder();for(byte b:values)out.append(String.format("%02x",b));return out.toString();}
}
