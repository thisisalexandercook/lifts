package org.lift.repositories;

import java.sql.*;
import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;
import org.lift.models.Schedule;
import org.lift.models.User;
import org.lift.models.WorkoutType;
import org.lift.utils.DatabaseConnection;

public class ScheduleRepository {

  public void create(Schedule schedule) throws SQLException {

    String query =
        """
            INSERT INTO schedules (
                user_name, monday, tuesday, wednesday, thursday, friday, saturday, sunday
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, schedule.getUser().getName());
      Map<DayOfWeek, WorkoutType> weeklySchedule = schedule.getWeeklySchedule();
      stmt.setString(2, weeklySchedule.get(DayOfWeek.MONDAY).toString());
      stmt.setString(3, weeklySchedule.get(DayOfWeek.TUESDAY).toString());
      stmt.setString(4, weeklySchedule.get(DayOfWeek.WEDNESDAY).toString());
      stmt.setString(5, weeklySchedule.get(DayOfWeek.THURSDAY).toString());
      stmt.setString(6, weeklySchedule.get(DayOfWeek.FRIDAY).toString());
      stmt.setString(7, weeklySchedule.get(DayOfWeek.SATURDAY).toString());
      stmt.setString(8, weeklySchedule.get(DayOfWeek.SUNDAY).toString());
      stmt.executeUpdate();
    }
  }

  public void update(Schedule schedule) throws SQLException {
    String query =
        """
    UPDATE schedules
    SET monday = ?, tuesday = ?, wednesday = ?, thursday = ?, friday = ?, saturday = ?, sunday = ?
    WHERE user_name = ?
""";

    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
      Map<DayOfWeek, WorkoutType> weeklySchedule = schedule.getWeeklySchedule();
      stmt.setString(1, weeklySchedule.get(DayOfWeek.MONDAY).toString());
      stmt.setString(2, weeklySchedule.get(DayOfWeek.TUESDAY).toString());
      stmt.setString(3, weeklySchedule.get(DayOfWeek.WEDNESDAY).toString());
      stmt.setString(4, weeklySchedule.get(DayOfWeek.THURSDAY).toString());
      stmt.setString(5, weeklySchedule.get(DayOfWeek.FRIDAY).toString());
      stmt.setString(6, weeklySchedule.get(DayOfWeek.SATURDAY).toString());
      stmt.setString(7, weeklySchedule.get(DayOfWeek.SUNDAY).toString());
      stmt.setString(8, schedule.getUser().getName());
      stmt.executeUpdate();
    }
  }

  public Schedule find(User user) throws SQLException {
    String query = "SELECT * FROM schedules WHERE user_name = ?";

    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, user.getName());
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        Map<DayOfWeek, WorkoutType> weeklySchedule = new EnumMap<>(DayOfWeek.class);
        weeklySchedule.put(
            DayOfWeek.MONDAY, WorkoutType.valueOf(rs.getString("monday").toUpperCase()));
        weeklySchedule.put(
            DayOfWeek.TUESDAY, WorkoutType.valueOf(rs.getString("tuesday").toUpperCase()));
        weeklySchedule.put(
            DayOfWeek.WEDNESDAY, WorkoutType.valueOf(rs.getString("wednesday").toUpperCase()));
        weeklySchedule.put(
            DayOfWeek.THURSDAY, WorkoutType.valueOf(rs.getString("thursday").toUpperCase()));
        weeklySchedule.put(
            DayOfWeek.FRIDAY, WorkoutType.valueOf(rs.getString("friday").toUpperCase()));
        weeklySchedule.put(
            DayOfWeek.SATURDAY, WorkoutType.valueOf(rs.getString("saturday").toUpperCase()));
        weeklySchedule.put(
            DayOfWeek.SUNDAY, WorkoutType.valueOf(rs.getString("sunday").toUpperCase()));

        return new Schedule(user, weeklySchedule);
      }
    }
    return null; 
  }

  public boolean delete(String userName) throws SQLException {
    String query = "DELETE FROM schedules WHERE user_name = ?";

    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, userName);
      int rowsAffected = stmt.executeUpdate();
      return rowsAffected > 0; // True if a row was deleted, false otherwise
    }
  }
}
