package org.lift.commands;

import java.sql.SQLException;
import org.lift.models.User;
import org.lift.repositories.UserRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "adduser", description = "Add a new user to the database.")
public class AddUser implements Runnable {

  @Option(
      names = {"-n", "--name"},
      required = true,
      description = "The name of the user.")
  private String name;

  @Override
  public void run() {

    User user = new User(name);
    UserRepository userRepository = new UserRepository();

    try {
      userRepository.create(user);
      System.out.println("User added: " + user);
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062) {
        System.err.println("Error: User already exists.");
        return;
      } else {
        System.err.println("Error " + e.getErrorCode() + "adding user: " + e.getMessage());
      }
    }
  }
}
