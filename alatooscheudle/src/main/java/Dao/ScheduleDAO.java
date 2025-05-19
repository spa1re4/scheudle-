package Dao;

import java.sql.*;

public class ScheduleDAO {

    private static final String DB_URL = "jdbc:sqlite:schedule.db";

    // Подключение к базе данных
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // Получение расписания по дню недели
    public String getScheduleByDay(String day) {
        String sql = "SELECT content FROM schedule WHERE day = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, day);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("content");
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при получении расписания для дня: " + day);
            e.printStackTrace();
        }
        return "Расписание не найдено для " + day;
    }
}
