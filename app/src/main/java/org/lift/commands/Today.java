package org.lift.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.lift.utils.DatabaseConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "today", description = "Manage today's workout for the specified user.")
public class Today implements Runnable {

  @Option(
      names = {"-u", "--user-name"},
      required = true,
      description = "The user's name.")
  private String userName;

  @Option(
      names = {"--final-rep"},
      description = "Final rep count for set 3 (only valid for week 3).")
  private Integer finalRep;

  @Override
  public void run() {
    LocalDate today = LocalDate.now();

    try (Connection connection = DatabaseConnection.getConnection()) {
      // Step 1: Check if there's already a workout for today
      if (isWorkoutAlreadyCreated(connection, today)) {
        System.out.printf("Loading workout for '%s'.%n%n", today);
      } else {

        // Step 2: Determine the current cycle and week
        int[] cycleWeek = determineCycleAndWeek(connection);
        int currentCycle = cycleWeek[0];
        int currentWeek = cycleWeek[1];
        String workoutType = determineWorkoutType(connection, userName, today.getDayOfWeek());

        // Step 3: Create the workout
        createWorkout(connection, currentCycle, currentWeek, workoutType);

        // Step 4: Populate the workout information
        int workoutId = getCurrentWorkoutId(connection, userName);
        populateWorkout(connection, workoutId, workoutType, currentWeek, userName);
      }
      // Optionally update the final rep count for week 3 workouts
      if (finalRep != null) {
        handleFinalRepOption(connection, userName, finalRep);
      }
      // Step 5: Print the workout summary
      printWorkoutSummary(connection, userName);

    } catch (SQLException e) {
      System.err.println("Error managing today's workout: " + e.getMessage());
    }
  }

  private void populateWorkout(
      Connection connection, int workoutId, String workoutType, int currentWeek, String userName)
      throws SQLException {
    if (workoutType.equalsIgnoreCase("misc") || workoutType.equalsIgnoreCase("rest")) {
      System.out.printf("Workout type '%s' does not require sets. Skipping.%n", workoutType);
      return;
    }

    switch (workoutType.toLowerCase()) {
      case "push":
      case "pull":
      case "legs":
        populateWeightWorkout(connection, workoutId, currentWeek, workoutType, userName);
        break;
      case "cardio":
        populateCardioWorkout(connection, workoutId);
        break;
      default:
        System.err.printf("Invalid workout type: '%s'. Skipping.%n", workoutType);
    }
  }

  private void handleFinalRepOption(Connection connection, String userName, Integer finalRep)
      throws SQLException {
    if (finalRep == null) {
      return; // Do nothing if finalRep is not specified
    }

    // Query to get the current workout and weight workout details for the specified
    // user
    String query =
        """
            SELECT w.id AS workout_id, w.week_number, w.workout_type, ww.set3_weight
            FROM workouts w
            JOIN weight_workouts ww ON w.id = ww.workout_id
            WHERE w.user_name = ? AND DATE(w.created_at) = CURRENT_DATE
        """;

    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, userName);

      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        int workoutId = rs.getInt("workout_id");
        int currentWeek = rs.getInt("week_number");
        String workoutType = rs.getString("workout_type");
        double finalSetWeight = rs.getDouble("set3_weight");

        if (currentWeek != 3) {
          throw new IllegalArgumentException("The --final-rep option is only valid for week 3.");
        }

        // Update the weight_workouts table with the final rep count
        String updateQuery = "UPDATE weight_workouts SET final_reps = ? WHERE workout_id = ?";
        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
          updateStmt.setInt(1, finalRep);
          updateStmt.setInt(2, workoutId);
          int rowsUpdated = updateStmt.executeUpdate();

          if (rowsUpdated > 0) {
            // Map workout type to lift type
            String liftType = mapWorkoutTypeToLiftType(workoutType);

            // Calculate the new max and add it to the database
            calculateNewMax(connection, userName, liftType, finalSetWeight, finalRep);
          } else {
            System.out.printf(
                "No weight workout found for workout ID %d to update final reps.%n", workoutId);
          }
        }
      } else {
        System.out.printf(
            "No workout found for user '%s' on today's date to update final reps.%n", userName);
      }
    }
  }

  private void calculateNewMax(
      Connection connection, String userName, String liftType, double finalSetWeight, int finalRep)
      throws SQLException {
    // Calculate the new max using the formula
    double newMax = finalSetWeight * (1 + 0.033 * finalRep);

    // Insert the new max into the max_lifts table
    String insertQuery =
        """
            INSERT INTO max_lifts (user_name, lift_type, max_weight, created_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        """;

    try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
      stmt.setString(1, userName);
      stmt.setString(2, liftType);
      stmt.setDouble(3, newMax);
      stmt.executeUpdate();

      System.out.printf(
          "New max for %s (%s): %.2f lbs successfully added.%n", userName, liftType, newMax);
    }
  }

  private void populateWeightWorkout(
      Connection connection, int workoutId, int currentWeek, String workoutType, String userName)
      throws SQLException {
    // Map workout types to their corresponding max lift type
    String liftType;
    switch (workoutType.toLowerCase()) {
      case "push":
        liftType = "bench";
        break;
      case "pull":
        liftType = "deadlift";
        break;
      case "legs":
        liftType = "squat";
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid workout type for weight workout: " + workoutType);
    }

    // Retrieve the most recent max for the given lift type
    double maxWeight = getMostRecentMaxWeight(connection, liftType, userName);

    // Calculate set weights based on the current week
    double[] percentages = getWeekPercentages(currentWeek);
    double set1Weight = maxWeight * percentages[0];
    double set2Weight = maxWeight * percentages[1];
    double set3Weight = maxWeight * percentages[2];

    // Final reps only for week 3
    Integer finalReps = (currentWeek == 3) ? 8 : null;

    // Insert the weight workout into the database
    String insertWeightWorkoutSQL =
        "INSERT INTO weight_workouts (workout_id, set1_weight, set2_weight, set3_weight,"
            + " final_reps) VALUES (?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = connection.prepareStatement(insertWeightWorkoutSQL)) {
      stmt.setInt(1, workoutId);
      stmt.setDouble(2, set1Weight);
      stmt.setDouble(3, set2Weight);
      stmt.setDouble(4, set3Weight);
      if (finalReps != null) {
        stmt.setInt(5, finalReps);
      } else {
        stmt.setNull(5, java.sql.Types.INTEGER);
      }
      stmt.executeUpdate();
    }
  }

  private double getMostRecentMaxWeight(Connection connection, String liftType, String userName)
      throws SQLException {
    String query =
        "SELECT max_weight FROM max_lifts "
            + "WHERE lift_type = ? AND user_name = ? "
            + "ORDER BY created_at DESC LIMIT 1";

    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, liftType);
      stmt.setString(2, userName);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getDouble("max_weight");
      } else {
        throw new SQLException(
            "No max weight found for user '" + userName + "' and lift type: '" + liftType + "'");
      }
    }
  }

  private double[] getWeekPercentages(int currentWeek) {
    switch (currentWeek) {
      case 1:
        return new double[] {0.65, 0.75, 0.85};
      case 2:
        return new double[] {0.75, 0.80, 0.90};
      case 3:
        return new double[] {0.75, 0.85, 0.95};
      case 4:
        return new double[] {0.40, 0.50, 0.60};
      default:
        throw new IllegalArgumentException("Invalid week number: " + currentWeek);
    }
  }

  private void populateCardioWorkout(Connection connection, int workoutId) throws SQLException {
    // Example cardio details (can be adjusted based on requirements)
    int durationMinutes = 30; // Example: 30 minutes
    int avgHeartRate = 140; // Example: Average heart rate

    String insertCardioWorkoutSQL =
        "INSERT INTO cardio_workout (workout_id, duration_minutes, avg_heart_rate) "
            + "VALUES (?, ?, ?)";

    try (PreparedStatement stmt = connection.prepareStatement(insertCardioWorkoutSQL)) {
      stmt.setInt(1, workoutId);
      stmt.setInt(2, durationMinutes);
      stmt.setInt(3, avgHeartRate);
      stmt.executeUpdate();

      System.out.printf("Cardio workout populated for workout ID %d.%n", workoutId);
    }
  }

  private int getCurrentWorkoutId(Connection connection, String userName) throws SQLException {
    String query =
        "SELECT id FROM workouts WHERE user_name = ? AND DATE(created_at) = CURRENT_DATE";

    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, userName);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getInt("id");
      } else {
        throw new SQLException("No workout found for user '" + userName + "' on today's date.");
      }
    }
  }

  private boolean isWorkoutAlreadyCreated(Connection connection, LocalDate today)
      throws SQLException {
    String query =
        "SELECT COUNT(*) AS count FROM workouts WHERE user_name = ? AND DATE(created_at) = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, userName);
      stmt.setDate(2, java.sql.Date.valueOf(today));

      ResultSet rs = stmt.executeQuery();
      if (rs.next() && rs.getInt("count") > 0) {
        return true;
      }
    }
    return false;
  }

  private void printWorkoutSummary(Connection connection, String userName) throws SQLException {
    // Query to fetch the workout and weight workout details
    String workoutQuery =
        """
    SELECT w.id AS workout_id, w.cycle_number, w.week_number, w.workout_type, ww.set1_weight,
           ww.set2_weight, ww.set3_weight, ww.final_reps
    FROM workouts w
    LEFT JOIN weight_workouts ww ON w.id = ww.workout_id
    WHERE w.user_name = ? AND DATE(w.created_at) = CURRENT_DATE
""";

    try (PreparedStatement stmt = connection.prepareStatement(workoutQuery)) {
      stmt.setString(1, userName);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        // Extract workout details
        int workoutId = rs.getInt("workout_id");
        int currentCycle = rs.getInt("cycle_number");
        int currentWeek = rs.getInt("week_number");
        String workoutType = rs.getString("workout_type");
        double set1Weight = rs.getDouble("set1_weight");
        double set2Weight = rs.getDouble("set2_weight");
        double set3Weight = rs.getDouble("set3_weight");
        Integer finalReps = rs.getObject("final_reps", Integer.class); // Handle NULL for final reps

        if (workoutType.equalsIgnoreCase("rest")) {
          System.out.println("Today is a rest day. No workout scheduled.");
          return;
        }

        // Map workout type to lift type
        String liftType = mapWorkoutTypeToLiftType(workoutType);

        // Print summary
        System.out.printf("Cycle %d Week %d%n", currentCycle, currentWeek);
        System.out.printf(
            "Today's Workout: %s. Today's Lift: %s.%n",
            capitalize(workoutType), capitalize(liftType));
        System.out.printf("Set 1: %.2f lbs%n", set1Weight);
        System.out.printf("Set 2: %.2f lbs%n", set2Weight);
        System.out.printf("Set 3: %.2f lbs", set3Weight);

        // Add final reps if applicable
        if (finalReps != null) {
          System.out.printf(" (%d reps)%n", finalReps);
        } else {
          System.out.println();
        }
      } else {
        System.out.printf("No workout found for user '%s' on today's date.%n", userName);
      }
    }
  }

  private String mapWorkoutTypeToLiftType(String workoutType) {
    switch (workoutType.toLowerCase()) {
      case "push":
        return "bench";
      case "pull":
        return "deadlift";
      case "legs":
        return "squat";
      default:
        return "N/A"; // For misc or cardio
    }
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  private int[] determineCycleAndWeek(Connection connection) throws SQLException {
    String query =
        "SELECT cycle_number, week_number, created_at FROM workouts "
            + "WHERE user_name = ? ORDER BY created_at DESC LIMIT 1";

    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, userName);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        int lastCycle = rs.getInt("cycle_number");
        int lastWeek = rs.getInt("week_number");
        LocalDate lastWorkoutDate = rs.getTimestamp("created_at").toLocalDateTime().toLocalDate();

        // Get day of the week for the last workout and today
        DayOfWeek lastWorkoutDay = lastWorkoutDate.getDayOfWeek();
        DayOfWeek todayDay = LocalDate.now().getDayOfWeek();

        // Calculate the difference in days between the two dates
        long daysSinceLastWorkout = ChronoUnit.DAYS.between(lastWorkoutDate, LocalDate.now());

        // Determine if we're in a new week based on day of the week
        boolean isNewWeek =
            daysSinceLastWorkout > 0 && (todayDay.getValue() <= lastWorkoutDay.getValue());

        // Calculate the new week and cycle
        int additionalWeeks = (int) daysSinceLastWorkout / 7;
        int newWeek = lastWeek + additionalWeeks + (isNewWeek ? 1 : 0);
        int newCycle = lastCycle + (newWeek - 1) / 4;
        newWeek = (newWeek - 1) % 4 + 1;

        return new int[] {newCycle, newWeek};
      }
    }

    // No previous workouts: Start with cycle 1, week 1
    return new int[] {1, 1};
  }

  private String determineWorkoutType(Connection connection, String userName, DayOfWeek dayOfWeek)
      throws SQLException {
    // Query to get the schedule for the user
    String query =
        "SELECT " + dayOfWeek.name() + " AS workout_type FROM schedules WHERE user_name = ?";

    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, userName);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getString("workout_type");
      } else {
        throw new SQLException("No schedule found for user: " + userName);
      }
    }
  }

  private void createWorkout(Connection connection, int cycle, int week, String workoutType)
      throws SQLException {
    String insertQuery =
        "INSERT INTO workouts (user_name, cycle_number, week_number, workout_type) "
            + "VALUES (?, ?, ?, ?)"; // Default to 'misc' or customize as needed

    try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
      stmt.setString(1, userName);
      stmt.setInt(2, cycle);
      stmt.setInt(3, week);
      stmt.setString(4, workoutType);
      stmt.executeUpdate();
      System.out.printf(
          "Workout created for '%s', Date %s%n",
          userName, LocalDate.now());
    }
  }
}
