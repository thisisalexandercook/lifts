package org.lift.models;

import java.util.Map;

class MaxLifts {
  private User user;
  private Map<String, Double> maxLifts;

  public MaxLifts(User user, Map<String, Double> maxLifts) {
    this.user = user;
    this.maxLifts = maxLifts;
  }
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Map<String, Double> getMaxLifts() {
    return maxLifts;
  }

  public void setMaxLifts(Map<String, Double> maxLifts) {
    this.maxLifts = maxLifts;
  }
}
