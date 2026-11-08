package com.ale.tuney.dao;

import com.ale.tuney.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FollowDAO {

    public void follow(int followerId, int followedId) throws SQLException {
        String query = "INSERT INTO follows (follower_id, followed_id) VALUES (?, ?);";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, followerId);
        stmt.setInt(2, followedId);
        stmt.execute();
        stmt.close();
    }

    public void unfollow(int followerId, int followedId) throws SQLException {
        String query = "DELETE FROM follows WHERE follower_id = ? AND followed_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, followerId);
        stmt.setInt(2, followedId);
        stmt.execute();
        stmt.close();
    }

    public boolean isFollowing(int followerId, int followedId) throws SQLException {
        String query = "SELECT 1 FROM follows WHERE follower_id = ? AND followed_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, followerId);
        stmt.setInt(2, followedId);
        return stmt.executeQuery().next();
    }

    public int getFollowerCount(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM follows WHERE followed_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int getFollowingCount(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM follows WHERE follower_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public List<User> getFollowers(int userId) throws SQLException {
        String query = "SELECT u.* FROM users u JOIN follows f ON u.id = f.follower_id WHERE f.followed_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<User> followers = new ArrayList<>();
        while (rs.next()) {
            followers.add(new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            ));
        }
        return followers;
    }

    public List<User> getFollowing(int userId) throws SQLException {
        String query = "SELECT u.* FROM users u JOIN follows f ON u.id = f.followed_id WHERE f.follower_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<User> following = new ArrayList<>();
        while (rs.next()) {
            following.add(new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            ));
        }
        return following;
    }
}
