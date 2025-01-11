package org.lift.utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory(System.getProperty("user.dir")) // Ensure correct directory
            .filename("credentials.env")               // Use your specific file name
            .load();

    private static final String URL = "jdbc:mysql://localhost:3306/lift_tracker";
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");

    private DatabaseConnection() {
        // Prevent instantiation
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
