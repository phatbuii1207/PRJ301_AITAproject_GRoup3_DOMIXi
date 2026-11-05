package com.fpt.aita.dal;

import com.fpt.aita.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Shared PostgreSQL JDBC configuration. No credentials or SQL Server fallback in source. */
public class DBContext implements AutoCloseable {
    // DAOs are stateless: constructing one never opens or retains a database session.
    public DBContext() { }

    public static Connection openConnection() throws SQLException {
        String url = AppConfig.get("DB_URL", "jdbc:postgresql://" + AppConfig.get("DB_HOST", "localhost")
                + ":" + AppConfig.get("DB_PORT", "5432") + "/" + AppConfig.get("DB_NAME", "AST_CodeAnalysisDB"));
        if (!url.startsWith("jdbc:postgresql:")) throw new SQLException("DB_URL must use PostgreSQL", "08001");
        String user = AppConfig.get("DB_USER", "aita"), password = AppConfig.get("DB_PASSWORD", "");
        if (password.isEmpty()) throw new SQLException("DB_PASSWORD is required", "08001");
        try { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException ex) { throw new SQLException("PostgreSQL JDBC driver missing", "08001"); }
        Properties properties = new Properties();
        properties.setProperty("user", user); properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "10"); properties.setProperty("socketTimeout", "30");
        properties.setProperty("ApplicationName", "AITA");
        return DriverManager.getConnection(url, properties);
    }
    /** Caller owns and must close this connection. Prefer openConnection() explicitly. */
    public Connection getConnection() throws SQLException { return openConnection(); }
    @Override public void close() throws SQLException { /* No retained connection. */ }
    public static void main(String[] args) throws SQLException {
        try (Connection db = openConnection()) {
            System.out.println("KẾT NỐI POSTGRESQL THÀNH CÔNG: " + db.getMetaData().getDatabaseProductName());
        }
    }
}
