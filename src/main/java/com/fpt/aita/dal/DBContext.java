package com.fpt.aita.dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBContext {
    protected Connection connection;

    public DBContext() {
        try {
            // Thay đổi user và password theo tài khoản SQL Server của bạn
            
            String user = "sa";
            String pass = "123456@Password";
            String host = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
            String url = "jdbc:sqlserver://" + host + ":1433;databaseName=AST_CodeAnalysisDB;encrypt=true;trustServerCertificate=true";

            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("Lỗi kết nối CSDL: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    public static void main(String[] args) {
    DBContext db = new DBContext();
    if (db.connection != null) {
        System.out.println("===> KẾT NỐI DATABASE THÀNH CÔNG!");
    } else {
        System.out.println("===> KẾT NỐI DATABASE THẤT BẠI! Hãy kiểm tra user/pass/port.");
    }
}

}
