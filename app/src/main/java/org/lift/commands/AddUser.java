package org.lift.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.lift.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Command(name = "adduser", description = "Add a new user to the database.")
public class AddUser implements Runnable {

    @Option(names = {"-n", "--name"}, required = true, description = "The name of the user.")
    private String name;

    @Override
    public void run() {
        // SQL query to insert a new user
        String insertUserSQL = "INSERT INTO users (name) VALUES (?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(insertUserSQL)) {

            stmt.setString(1, name);
            stmt.executeUpdate();

            System.out.printf("User '%s' has been successfully added.%n", name);

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry error code
                System.err.printf("Error: User '%s' already exists.%n", name);
            } else {
                System.err.println("Error adding user to the database: " + e.getMessage());
            }
        }
    }
}
