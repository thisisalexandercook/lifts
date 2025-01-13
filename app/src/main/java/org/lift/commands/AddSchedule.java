package org.lift.commands;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.EnumMap;
import org.lift.models.Schedule;
import org.lift.models.User;
import org.lift.models.WorkoutType;
import org.lift.repositories.ScheduleRepository;
import org.lift.repositories.UserRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "addschedule", description = "Add a new schedule for a user.")
public class AddSchedule implements Runnable {

  @Option(
      names = {"-u", "--user-name"},
      required = true,
      description = "The name of the user.")
  private String userName;

  @Option(
      names = {"--monday"},
      required = true,
      description = "Workout type for Monday.")
  private String monday;

  @Option(
      names = {"--tuesday"},
      required = true,
      description = "Workout type for Tuesday.")
  private String tuesday;

  @Option(
      names = {"--wednesday"},
      required = true,
      description = "Workout type for Wednesday.")
  private String wednesday;

  @Option(
      names = {"--thursday"},
      required = true,
      description = "Workout type for Thursday.")
  private String thursday;

  @Option(
      names = {"--friday"},
      required = true,
      description = "Workout type for Friday.")
  private String friday;

  @Option(
      names = {"--saturday"},
      required = true,
      description = "Workout type for Saturday.")
  private String saturday;

  @Option(
      names = {"--sunday"},
      required = true,
      description = "Workout type for Sunday.")
  private String sunday;

  @Override
  public void run() {
    UserRepository userRepo = new UserRepository();
    User user;

    try {
      user = userRepo.find(userName);

      if (user == null) {
        System.err.printf("Error: User %s does not exist.%n", userName);
        return;
      }

    } catch (SQLException e) {
      System.err.printf(
          "Error: An error occured while trying to fetch user '%s' schedule", userName);
      return;
    }

    ScheduleRepository scheduleRepo = new ScheduleRepository();
    Schedule schedule;

    try {
        schedule = scheduleRepo.find(user);
    
        if (schedule != null) {
            System.err.printf("Error: User '%s' already has a schedule.%n", userName);
            return;
        }
    
        } catch (SQLException e) {
        System.err.printf(
            "Error: An error occured while trying to fetch user '%s' schedule", userName);
        return;
    }

    EnumMap<DayOfWeek, WorkoutType> weeklySchedule = new EnumMap<>(DayOfWeek.class);

    try {
      // Map workout types to days
      weeklySchedule.put(DayOfWeek.MONDAY, WorkoutType.valueOf(monday.toUpperCase()));
      weeklySchedule.put(DayOfWeek.TUESDAY, WorkoutType.valueOf(tuesday.toUpperCase()));
      weeklySchedule.put(DayOfWeek.WEDNESDAY, WorkoutType.valueOf(wednesday.toUpperCase()));
      weeklySchedule.put(DayOfWeek.THURSDAY, WorkoutType.valueOf(thursday.toUpperCase()));
      weeklySchedule.put(DayOfWeek.FRIDAY, WorkoutType.valueOf(friday.toUpperCase()));
      weeklySchedule.put(DayOfWeek.SATURDAY, WorkoutType.valueOf(saturday.toUpperCase()));
      weeklySchedule.put(DayOfWeek.SUNDAY, WorkoutType.valueOf(sunday.toUpperCase()));

      schedule = new Schedule(user, weeklySchedule);
      scheduleRepo.create(schedule);

      System.out.printf("Schedule for user '%s' has been successfully added.%n", userName);
    } catch (IllegalArgumentException e) {
      System.err.printf("Error: Invalid workout type specified. %s%n", e.getMessage());
    } catch (SQLException e) {
      System.err.printf("An error occurred while adding the schedule: %s%n", e.getMessage());
    }
  }
}
