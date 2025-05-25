package org.example;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    // Путь к файлу базы данных (создастся автоматически, если его нет)
    private static final String DB_URL = "jdbc:sqlite:identifier.sqlite";
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
