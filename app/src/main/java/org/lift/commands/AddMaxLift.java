package org.lift.commands;

import org.lift.utils.DatabaseConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Command(name = "addmaxlift", description = "Add a max lift record for a specific lift type.")
public class AddMaxLift implements Runnable {

    @Option(names = {"-u", "--user-name"}, required = true, description = "The name of the user.")
    private String userName;

    @Option(names = {"-t", "--lift-type"}, required = true, description = "The type of lift (bench, squat, deadlift).")
    private String liftType;

    @Option(names = {"-w", "--weight"}, required = true, description = "The maximum weight lifted.")
    private double maxWeight;

    @Override
    public void run() {
        // SQL to insert a max lift record
        String insertMaxLiftSQL = "INSERT INTO max_lifts (user_name, lift_type, max_weight) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(insertMaxLiftSQL)) {

            // Validate lift type
            if (!isValidLiftType(liftType)) {
                throw new IllegalArgumentException("Invalid lift type: " + liftType);
            }

            // Set query parameters
            stmt.setString(1, userName);
            stmt.setString(2, liftType.toLowerCase());
            stmt.setDouble(3, maxWeight);

            // Execute the query
            stmt.executeUpdate();

            System.out.printf("Max lift added: User '%s', Lift '%s', Weight %.2f%n", userName, liftType, maxWeight);

        } catch (SQLException e) {
            System.err.println("Error adding max lift: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private boolean isValidLiftType(String liftType) {
        return liftType.equalsIgnoreCase("bench") ||
               liftType.equalsIgnoreCase("squat") ||
               liftType.equalsIgnoreCase("deadlift");
    }
}
