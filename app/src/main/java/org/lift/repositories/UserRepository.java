package org.lift.repositories;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.lift.models.User;
import org.lift.utils.DatabaseConnection;

public class UserRepository {

  public User find(String name) throws SQLException {
    String query = "SELECT * FROM users WHERE name = ?";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, name);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return new User(rs.getString("name"));
      }
    }
    return null;
  }

  public List<User> findAll() throws SQLException {
    String query = "SELECT * FROM users";
    List<User> users = new ArrayList<>();
    try (Connection connection = DatabaseConnection.getConnection();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {
      while (rs.next()) {
        users.add(new User(rs.getString("name")));
      }
    }
    return users;
  }

  public void create(User user) throws SQLException {
    String query = "INSERT INTO users (name) VALUES (?)";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, user.getName());
      stmt.executeUpdate();
    }
  }

  public boolean delete(User user) throws SQLException {
    String query = "DELETE FROM users WHERE name = ?";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, user.getName());
      int rowsAffected = stmt.executeUpdate();
      return rowsAffected > 0; 
    }
  }
}
