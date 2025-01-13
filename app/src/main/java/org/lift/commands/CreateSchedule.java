package org.lift.commands;

import org.lift.utils.DatabaseConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Command(name = "createschedule", description = "Create a new weekly schedule for a user.")
public class CreateSchedule implements Runnable {

    @Option(names = {"-u", "--user-name"}, required = true, description = "The name of the user.")
    private String userName;

    @Option(names = {"--monday"}, required = true, description = "Workout type for Monday (push, pull, cardio, legs, misc, rest).")
    private String monday;

    @Option(names = {"--tuesday"}, required = true, description = "Workout type for Tuesday (push, pull, cardio, legs, misc, rest).")
    private String tuesday;

    @Option(names = {"--wednesday"}, required = true, description = "Workout type for Wednesday (push, pull, cardio, legs, misc, rest).")
    private String wednesday;

    @Option(names = {"--thursday"}, required = true, description = "Workout type for Thursday (push, pull, cardio, legs, misc, rest).")
    private String thursday;

    @Option(names = {"--friday"}, required = true, description = "Workout type for Friday (push, pull, cardio, legs, misc, rest).")
    private String friday;

    @Option(names = {"--saturday"}, required = true, description = "Workout type for Saturday (push, pull, cardio, legs, misc, rest).")
    private String saturday;

    @Option(names = {"--sunday"}, required = true, description = "Workout type for Sunday (push, pull, cardio, legs, misc, rest).")
    private String sunday;

    @Override
    public void run() {
        String insertScheduleSQL = "INSERT INTO schedules " +
                "(user_name, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(insertScheduleSQL)) {

            // Validate workout types for each day
            validateWorkoutType(monday, "Monday");
            validateWorkoutType(tuesday, "Tuesday");
            validateWorkoutType(wednesday, "Wednesday");
            validateWorkoutType(thursday, "Thursday");
            validateWorkoutType(friday, "Friday");
            validateWorkoutType(saturday, "Saturday");
            validateWorkoutType(sunday, "Sunday");

            // Set parameters for the query
            stmt.setString(1, userName);
            stmt.setString(2, monday);
            stmt.setString(3, tuesday);
            stmt.setString(4, wednesday);
            stmt.setString(5, thursday);
            stmt.setString(6, friday);
            stmt.setString(7, saturday);
            stmt.setString(8, sunday);

            // Execute the query
            stmt.executeUpdate();

            System.out.printf("Schedule created for user '%s'.%n", userName);

        } catch (SQLException e) {
            System.err.println("Error creating schedule: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid workout type: " + e.getMessage());
        }
    }

    private void validateWorkoutType(String workoutType, String day) {
        if (!isValidWorkoutType(workoutType)) {
            throw new IllegalArgumentException("Invalid workout type for " + day + ": " + workoutType);
        }
    }

    private boolean isValidWorkoutType(String workoutType) {
        return workoutType.equalsIgnoreCase("push") ||
               workoutType.equalsIgnoreCase("pull") ||
               workoutType.equalsIgnoreCase("cardio") ||
               workoutType.equalsIgnoreCase("legs") ||
               workoutType.equalsIgnoreCase("misc") ||
               workoutType.equalsIgnoreCase("rest");
    }
}
