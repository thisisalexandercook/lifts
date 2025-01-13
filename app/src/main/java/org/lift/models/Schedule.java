package org.lift.models;

import java.time.DayOfWeek;
import java.util.Map;

public class Schedule {
  private User user;
  private Map<DayOfWeek, WorkoutType> weeklySchedule;

  public Schedule(User user, Map<DayOfWeek, WorkoutType> weeklySchedule) {
    this.user = user;
    this.weeklySchedule = weeklySchedule;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Map<DayOfWeek, WorkoutType> getWeeklySchedule() {
    return weeklySchedule;
  }

  public void setWeeklySchedule(Map<DayOfWeek, WorkoutType> weeklySchedule) {
    this.weeklySchedule = weeklySchedule;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Schedule for ").append(user.getName()).append(":\n");
    weeklySchedule.forEach(
        (day, workoutType) -> sb.append(day).append(": ").append(workoutType).append("\n"));
    return sb.toString();
  }
}
