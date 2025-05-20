package Dao;

import java.sql.*;

public class ScheduleDAO {
    private static final String DB_URL = "jdbc:sqlite:schedule.db";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public String getSchedule(String direction, String day) {
        String sql = "SELECT content FROM schedule WHERE direction = ? AND day = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, direction);
            stmt.setString(2, day);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("content");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Расписание не найдено.";
    }
}
