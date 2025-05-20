package Dao;

import java.sql.*;

public class UserDAO {
    private static final String DB_URL = "jdbc:sqlite:schedule.db";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public boolean registerUser(String idCard, String password) {
        String sql = "INSERT INTO users (idCard, password) VALUES (?, ?)";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idCard);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false; // Возможно, пользователь уже существует
        }
    }

    public boolean userExists(String idCard, String password) {
        String sql = "SELECT * FROM users WHERE idCard = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idCard);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
}
