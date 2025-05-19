package Dao;

import java.sql.*;

public class UserDAO {

    private static final String DB_URL = "jdbc:sqlite:schedule.db";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public boolean userExists(String idCard, String password) {
        String sql = "SELECT 1 FROM users WHERE idCard = ? AND password = ? LIMIT 1";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idCard);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // если есть хотя бы одна строка — пользователь найден
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean registerUser(String idCard, String password, String direction) {
        String sql = "INSERT INTO users(idCard, password, direction) VALUES (?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idCard);
            stmt.setString(2, password);
            stmt.setString(3, direction);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
