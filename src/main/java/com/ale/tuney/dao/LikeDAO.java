package com.ale.tuney.dao;

import com.ale.tuney.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LikeDAO {

    public void like(int userId, int reviewId) throws SQLException {
        String query = "INSERT INTO likes (user_id, review_id) VALUES (?, ?);";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setInt(2, reviewId);
        stmt.execute();
        stmt.close();
    }

    public void unlike(int userId, int reviewId) throws SQLException {
        String query = "DELETE FROM likes WHERE user_id = ? AND review_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setInt(2, reviewId);
        stmt.execute();
        stmt.close();

        // rimuove anche l'attività associata
        new ActivityDAO().deleteByTypeAndUserAndReferenceId(userId, "like", reviewId);
    }

    // controlla se un utente ha già messo like per mostrare il bottone like/unlike
    public boolean hasLiked(int userId, int reviewId) throws SQLException {
        String query = "SELECT 1 FROM likes WHERE user_id = ? AND review_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setInt(2, reviewId);
        return stmt.executeQuery().next();
    }

    public int getLikeCount(int reviewId) throws SQLException {
        String query = "SELECT COUNT(*) FROM likes WHERE review_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, reviewId);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public List<User> getLikers(int reviewId) throws SQLException {
        String query = "SELECT u.* FROM users u JOIN likes l ON u.id = l.user_id WHERE l.review_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, reviewId);
        ResultSet rs = stmt.executeQuery();
        List<User> likers = new ArrayList<>();
        while (rs.next()) {
            likers.add(new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    "",
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            ));
        }
        return likers;
    }
}
