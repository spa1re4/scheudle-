package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public static boolean registerUser(long id, String password, String groupName) {
        String sql = "INSERT INTO users (id, password, group_name) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 🔍 Выводим значения для отладки
            System.out.println("📌 Регистрируем: chatId = " + id);
            System.out.println("📦 Пароль: " + password + ", Группа: " + groupName);

            pstmt.setLong(1, id);
            pstmt.setString(2, password);
            pstmt.setString(3, groupName);
            pstmt.executeUpdate();

            return true;

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при регистрации: " + e.getMessage());
            return false;
        }
    }

    public static boolean validateUser(long id, String password) {
        String sql = "SELECT * FROM users WHERE id = ? AND password = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            return rs.next(); // если есть результат, то данные верны

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при проверке логина: " + e.getMessage());
            return false;
        }
    }

    public static String getUserGroup(long id) {
        String sql = "SELECT group_name FROM users WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("group_name");
            }

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при получении группы: " + e.getMessage());
        }

        return null;
    }

    public static List<Long> getAllUserChatIds() {
        List<Long> chatIds = new ArrayList<>();
        String sql = "SELECT id FROM users";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                chatIds.add(rs.getLong("id"));
            }

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при получении всех пользователей: " + e.getMessage());
        }

        return chatIds;
    }

    public static List<Long> getUsersByGroup(String group) {
        List<Long> userIds = new ArrayList<>();
        String sql = "SELECT id FROM users WHERE group_name = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, group);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                userIds.add(rs.getLong("id"));
            }

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при получении пользователей по группе: " + e.getMessage());
        }

        return userIds;
    }
}