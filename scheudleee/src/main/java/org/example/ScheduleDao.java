package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDao {

    // Получение расписания по группе и дню
    public static String getSchedule(String groupName, String day) {
        String sql = "SELECT schedule_text FROM schedule WHERE group_name = ? AND day = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupName);
            pstmt.setString(2, day);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("schedule_text");
            } else {
                return "📭 Расписание не найдено для группы " + groupName + " на " + day + ".";
            }

        } catch (SQLException e) {
            System.out.println("Ошибка при получении расписания: " + e.getMessage());
            return "❌ Ошибка при получении расписания.";
        }
    }

    // Обновление или вставка расписания с уведомлением пользователей
    public static boolean updateSchedule(String group, String day, String newSchedule, MyBot bot) {
        String checkSql = "SELECT 1 FROM schedule WHERE group_name = ? AND day = ?";
        String updateSql = "UPDATE schedule SET schedule_text = ? WHERE group_name = ? AND day = ?";
        String insertSql = "INSERT INTO schedule (group_name, day, schedule_text) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection()) {
            boolean exists;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, group);
                checkStmt.setString(2, day);
                exists = checkStmt.executeQuery().next();
            }

            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, newSchedule);
                    updateStmt.setString(2, group);
                    updateStmt.setString(3, day);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, group);
                    insertStmt.setString(2, day);
                    insertStmt.setString(3, newSchedule);
                    insertStmt.executeUpdate();
                }
            }

            // Отправка уведомления всем пользователям группы
            List<Long> userIds = UserDao.getUsersByGroup(group);
            for (Long userId : userIds) {
                bot.sendText(userId, "📢 Обновлено расписание на *" + day + "* для группы *" + group + "*:\n\n" + newSchedule);
            }

            return true;

        } catch (SQLException e) {
            System.out.println("Ошибка при обновлении расписания: " + e.getMessage());
            return false;
        }
    }

    // Получение пользователей по группе
    public static List<Long> getUserIdsByGroup(String groupName) {
        String sql = "SELECT id FROM users WHERE group_name = ?";
        List<Long> userIds = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                userIds.add(rs.getLong("id"));
            }

        } catch (SQLException e) {
            System.out.println("Ошибка при получении пользователей по группе: " + e.getMessage());
        }

        return userIds;
    }
}
