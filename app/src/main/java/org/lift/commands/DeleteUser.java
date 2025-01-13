package org.lift.commands;

import org.lift.models.User;
import org.lift.repositories.UserRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "deleteuser", description = "Delete a user from the database.")
public class DeleteUser implements Runnable {

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
      boolean success = userRepository.delete(user);
      if (success) {
        System.out.println("User deleted: " + user);
      } else {
        System.err.println("Error: User does not exist.");
      }
    } catch (Exception e) {
      System.err.println("Error deleting user: " + e.getMessage());
    }
  }
}
