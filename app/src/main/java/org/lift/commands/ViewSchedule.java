package org.lift.commands;

import java.sql.SQLException;
import org.lift.models.Schedule;
import org.lift.models.User;
import org.lift.repositories.ScheduleRepository;
import org.lift.repositories.UserRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "viewschedule", description = "View the schedule for a specific user.")
public class ViewSchedule implements Runnable {

  @Option(
      names = {"-u", "--user-name"},
      required = true,
      description = "The name of the user.")
  private String userName;

  @Override
  public void run() {
    UserRepository userRepo = new UserRepository();
    User user;

    try {
      user = userRepo.find(userName);

      if (user == null) {
        System.err.printf("Error: User '%s' does not exist.%n", userName);
        return;
      }
    } catch (SQLException e) {
      System.err.printf("An error occurred while retrieving the user: %s%n", e.getMessage());
      return;
    }

    ScheduleRepository scheduleRepo = new ScheduleRepository();
    Schedule schedule;

    try {
      schedule = scheduleRepo.find(user);

      if (schedule == null) {
        System.err.printf("Error: Schedule not found for user '%s'.%n", userName);
        return;
      }
      System.out.println(schedule);
    } catch (SQLException e) {
      System.err.printf("An error occurred while retrieving the schedule: %s%n", e.getMessage());
    }
  }
}
