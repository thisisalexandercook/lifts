package org.lift.commands;

import org.lift.utils.DatabaseConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Command(name = "createschedule", description = "Create a new schedule for a user.")
public class CreateSchedule implements Runnable {

    @Option(names = { "-u", "--user-name" }, required = true, description = "The user's name.")
    private String userName;

    @Option(names = { "-s", "--schedule-name" }, required = true, description = "The name of the schedule.")
    private String scheduleName;

    @Override
    public void run() {
        String fetchUserSQL = "SELECT name FROM users WHERE name = ?";
        String insertScheduleSQL = "INSERT INTO schedules (schedule_name, user_name) VALUES (?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            // Verify that the user exists
            try (PreparedStatement fetchStmt = connection.prepareStatement(fetchUserSQL)) {
                fetchStmt.setString(1, userName);
                ResultSet rs = fetchStmt.executeQuery();

                if (!rs.next()) {
                    throw new SQLException("User not found: " + userName);
                }
            }

            // Insert the schedule into the database
            try (PreparedStatement insertStmt = connection.prepareStatement(insertScheduleSQL)) {
                insertStmt.setString(1, scheduleName);
                insertStmt.setString(2, userName);
                insertStmt.executeUpdate();

                System.out.printf(
                        "Schedule '%s' created successfully for user '%s'.%n", scheduleName, userName);
                System.out.println("Use the `addscheduleinfo` command to populate the schedule with days.");
            }

        } catch (SQLException e) {
            System.err.println("Error creating the schedule: " + e.getMessage());
        }
    }
}
