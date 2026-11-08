package com.ale.tuney.dao;

import com.ale.tuney.model.User;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class UserDAO {

    public User findByUsername(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            );
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String query = "SELECT * FROM users WHERE email = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            );
        }
        return null;
    }

    public boolean usernameExists(String username) throws SQLException {
        String query = "SELECT id FROM users WHERE username = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    public boolean emailExists(String email) throws SQLException {
        String query = "SELECT id FROM users WHERE email = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    public void save(User user) throws SQLException {
        String query = "INSERT INTO users (username, email, password, name, bio, profile_image_url) VALUES (?, ?, ?, ?, ?, ?);";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setString(1, user.getUsername());
        stmt.setString(2, user.getEmail());
        stmt.setString(3, user.getPassword());
        stmt.setString(4, user.getName());
        stmt.setString(5, user.getBio());
        stmt.setString(6, user.getProfileImageUrl());
        stmt.execute();
        stmt.close();
    }

    public void updateProfile(User user) throws SQLException {
        String query = "UPDATE users SET name = ?, bio = ?, profile_image_url = ? WHERE id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setString(1, user.getName());
        stmt.setString(2, user.getBio());
        stmt.setString(3, user.getProfileImageUrl());
        stmt.setInt(4, user.getId());
        stmt.execute();
        stmt.close();
    }

    public List<User> findByUsernameOrName(String query) throws SQLException {
        String searchQuery = "%" + query + "%";
        String sql = "SELECT * FROM users WHERE username LIKE ? OR name LIKE ? ORDER BY username ASC LIMIT 50";

        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql);
        stmt.setString(1, searchQuery);
        stmt.setString(2, searchQuery);

        ResultSet rs = stmt.executeQuery();
        List<User> users = new ArrayList<>();

        while (rs.next()) {
            User user = new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            );
            users.add(user);
        }

        rs.close();
        stmt.close();
        return users;
    }

    public void updatePassword(int userId, String newHashedPassword) throws SQLException {
        String query = "UPDATE users SET password = ? WHERE id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, newHashedPassword);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void deleteUser(int userId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM saved_items WHERE user_id = ?;")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM reviews WHERE user_id = ?;")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM likes WHERE user_id = ?;")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM follows WHERE follower_id = ? OR followed_id = ?;")) {
                stmt.setInt(1, userId);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM comments WHERE user_id = ?;")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM activities WHERE user_id = ?;")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM user_favorites WHERE user_id = ?;")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?;")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }
}
