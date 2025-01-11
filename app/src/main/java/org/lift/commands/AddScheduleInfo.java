package org.lift.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.lift.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Command(name = "addscheduleinfo", description = "Add a week's workout information to an existing schedule.")
public class AddScheduleInfo implements Runnable {

    @Option(names = {"-u", "--user-name"}, required = true, description = "The user's name.")
    private String userName;

    @Option(names = {"-s", "--schedule-name"}, required = true, description = "The name of the schedule.")
    private String scheduleName;

    @Option(names = "--monday", required = true, description = "Workout type for Monday (push, pull, cardio, rest, misc, legs).")
    private String monday;

    @Option(names = "--tuesday", required = true, description = "Workout type for Tuesday (push, pull, cardio, rest, misc, legs).")
    private String tuesday;

    @Option(names = "--wednesday", required = true, description = "Workout type for Wednesday (push, pull, cardio, rest, misc, legs).")
    private String wednesday;

    @Option(names = "--thursday", required = true, description = "Workout type for Thursday (push, pull, cardio, rest, misc, legs).")
    private String thursday;

    @Option(names = "--friday", required = true, description = "Workout type for Friday (push, pull, cardio, rest, misc, legs).")
    private String friday;

    @Option(names = "--saturday", required = true, description = "Workout type for Saturday (push, pull, cardio, rest, misc, legs).")
    private String saturday;

    @Option(names = "--sunday", required = true, description = "Workout type for Sunday (push, pull, cardio, rest, misc, legs).")
    private String sunday;

    @Override
    public void run() {
        String fetchScheduleSQL = "SELECT * FROM schedules WHERE user_name = ? AND schedule_name = ?";
        String insertScheduleDaySQL = "INSERT INTO schedule_days (user_name, schedule_name, day_of_week, workout_type) VALUES (?, ?, ?, ?)";

        String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] workoutTypes = {monday, tuesday, wednesday, thursday, friday, saturday, sunday};

        try (Connection connection = DatabaseConnection.getConnection()) {
            // Verify that the schedule exists
            try (PreparedStatement fetchStmt = connection.prepareStatement(fetchScheduleSQL)) {
                fetchStmt.setString(1, userName);
                fetchStmt.setString(2, scheduleName);
                ResultSet rs = fetchStmt.executeQuery();

                if (!rs.next()) {
                    throw new SQLException("Schedule not found for user: " + userName + " and schedule name: " + scheduleName);
                }
            }

            // Add workout information for each day
            try (PreparedStatement insertStmt = connection.prepareStatement(insertScheduleDaySQL)) {
                for (int i = 0; i < daysOfWeek.length; i++) {
                    String day = daysOfWeek[i];
                    String workoutType = workoutTypes[i].toLowerCase();

                    // Validate workout type
                    if (!isValidWorkoutType(workoutType)) {
                        throw new IllegalArgumentException("Invalid workout type for " + day + ": " + workoutType);
                    }

                    insertStmt.setString(1, userName);
                    insertStmt.setString(2, scheduleName);
                    insertStmt.setString(3, day);
                    insertStmt.setString(4, workoutType);
                    insertStmt.executeUpdate();
                }

                System.out.printf("Workout information successfully added for schedule '%s' of user '%s'.%n", scheduleName, userName);
            }

        } catch (SQLException e) {
            System.err.println("Error adding schedule information: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private boolean isValidWorkoutType(String workoutType) {
        return workoutType.equals("push") || workoutType.equals("pull") ||
               workoutType.equals("cardio") || workoutType.equals("rest") ||
               workoutType.equals("misc") || workoutType.equals("legs");
    }
}
