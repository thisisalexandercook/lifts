package org.lift;

import org.lift.commands.AddMaxLift;
import org.lift.commands.AddSchedule;
import org.lift.commands.AddScheduleInfo;
import org.lift.commands.AddUser;
import org.lift.commands.CreateSchedule;
import org.lift.commands.DeleteUser;
import org.lift.commands.Today;
import org.lift.commands.ViewSchedule;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "Lift",
    mixinStandardHelpOptions = true,
    version = "Lift 1.0",
    description = "A CLI tool to track your lifts using the 5/3/1 method.",
    subcommands = {
      AddUser.class,
      CreateSchedule.class,
      AddScheduleInfo.class,
      AddMaxLift.class,
      Today.class,
      DeleteUser.class,
      AddSchedule.class,
      ViewSchedule.class
    } // Register the new command here
    )
public class App implements Runnable {

  @Option(
      names = {"-u", "--user"},
      description = "Specify the user ID.")
  private int userId;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new App()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    System.out.println("Welcome to the Lift Tracker CLI!");
    System.out.println("User ID: " + userId);
  }
}
