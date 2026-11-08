package com.ale.tuney.dao;

import com.ale.tuney.model.Activity;
import com.ale.tuney.model.Comment;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.Review;
import com.ale.tuney.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityDAO {

    public void save(int userId, String type, int referenceId) throws SQLException {
        String query = "INSERT INTO activities (user_id, type, reference_id) VALUES (?, ?, ?);";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setString(2, type);
        stmt.setInt(3, referenceId);
        stmt.execute();
        stmt.close();
    }

    public void save(int userId, String type, String referenceText) throws SQLException {
        String query = "INSERT INTO activities (user_id, type, reference_id, reference_text) VALUES (?, ?, 0, ?);";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setString(2, type);
        stmt.setString(3, referenceText);
        stmt.execute();
        stmt.close();
    }

    public void deleteByTypeAndUserAndReferenceId(int userId, String type, int referenceId) throws SQLException {
        String query = "DELETE FROM activities WHERE user_id = ? AND type = ? AND reference_id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, type);
            stmt.setInt(3, referenceId);
            stmt.execute();
        }
    }

    public void deleteByTypeAndUserAndReferenceText(int userId, String type, String referenceText) throws SQLException {
        String query = "DELETE FROM activities WHERE user_id = ? AND type = ? AND reference_text = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, type);
            stmt.setString(3, referenceText);
            stmt.execute();
        }
    }

    public void deleteByTypeAndReferenceId(String type, int referenceId) throws SQLException {
        String query = "DELETE FROM activities WHERE type = ? AND reference_id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, type);
            stmt.setInt(2, referenceId);
            stmt.execute();
        }
    }

    public List<Activity> findByFollowed(int userId, boolean newestFirst) throws SQLException {
        String order = newestFirst ? "DESC" : "ASC";
        String query = "SELECT a.type, a.reference_id, a.reference_text, a.created_at, " +
                "u.id AS u_id, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM activities a JOIN users u ON a.user_id = u.id " +
                "WHERE a.user_id IN (SELECT followed_id FROM follows WHERE follower_id = ?) AND a.type != 'follow' " +
                "ORDER BY a.created_at " + order + " LIMIT 50;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<Activity> activities = new ArrayList<>();
        ReviewDAO reviewDAO = new ReviewDAO();
        CommentDAO commentDAO = new CommentDAO();
        while (rs.next()) {
            User user = new User(
                rs.getInt("u_id"),
                rs.getString("email"),
                "",
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("bio"),
                rs.getString("profile_image_url")
            );
            String type = rs.getString("type");
            int referenceId = rs.getInt("reference_id");
            String referenceText = rs.getString("reference_text");
            String createdAt = rs.getString("created_at");

            Activity activity = new Activity(0, user, type, createdAt);
            activity.setReferenceText(referenceText);
            if ("review".equals(type)) {
                try {
                    Review review = reviewDAO.findById(referenceId);
                    if (review == null) {
                        continue;
                    }
                    activity.setReview(review);
                } catch (SQLException ignored) {
                    continue;
                }
            } else if ("like".equals(type)) {
                try {
                    Review review = reviewDAO.findById(referenceId);
                    if (review != null) activity.setReview(review);
                } catch (SQLException ignored) {}
            } else if ("comment".equals(type)) {
                try {
                    List<Comment> allComments = commentDAO.findByCommentId(referenceId);
                    if (!allComments.isEmpty()) {
                        Comment comment = allComments.get(0);
                        activity.setComment(comment);
                        Review review = reviewDAO.findById(comment.getReviewId());
                        if (review != null) activity.setReview(review);
                    }
                } catch (SQLException ignored) {}
            } else if ("favorite".equals(type) && referenceText != null) {
                try {
                    String itemTypeQuery = "SELECT type FROM spotify_items WHERE id = ?;";
                    try (PreparedStatement itemStmt = DatabaseManager.getConnection().prepareStatement(itemTypeQuery)) {
                        itemStmt.setString(1, referenceText);
                        try (ResultSet itemRs = itemStmt.executeQuery()) {
                            if (itemRs.next()) {
                                String itemType = itemRs.getString("type");
                                Item item = SpotifyItemDAO.findById(referenceText, itemType);
                                activity.setItem(item);
                            }
                        }
                    }
                } catch (SQLException ignored) {}
            }
            activities.add(activity);
        }
        stmt.close();
        return activities;
    }

    public List<Activity> findByUser(int userId) throws SQLException {
        return findByUser(userId, true);
    }

    public List<Activity> findByUser(int userId, boolean newestFirst) throws SQLException {
        String order = newestFirst ? "DESC" : "ASC";
        String query = "SELECT a.type, a.reference_id, a.reference_text, a.created_at, " +
                "u.id AS u_id, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM activities a JOIN users u ON a.user_id = u.id " +
                "WHERE a.user_id = ? AND a.type != 'follow' " +
                "ORDER BY a.created_at " + order + " LIMIT 50;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<Activity> activities = new ArrayList<>();
        ReviewDAO reviewDAO = new ReviewDAO();
        CommentDAO commentDAO = new CommentDAO();
        while (rs.next()) {
            User user = new User(
                rs.getInt("u_id"),
                rs.getString("email"),
                "",
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("bio"),
                rs.getString("profile_image_url")
            );
            String type = rs.getString("type");
            int referenceId = rs.getInt("reference_id");
            String referenceText = rs.getString("reference_text");
            String createdAt = rs.getString("created_at");

            Activity activity = new Activity(0, user, type, createdAt);
            activity.setReferenceText(referenceText);
            if ("review".equals(type)) {
                try {
                    Review review = reviewDAO.findById(referenceId);
                    if (review == null) {
                        continue;
                    }
                    activity.setReview(review);
                } catch (SQLException ignored) {
                    continue;
                }
            } else if ("like".equals(type)) {
                try {
                    Review review = reviewDAO.findById(referenceId);
                    if (review != null) activity.setReview(review);
                } catch (SQLException ignored) {}
            } else if ("comment".equals(type)) {
                try {
                    List<Comment> allComments = commentDAO.findByCommentId(referenceId);
                    if (!allComments.isEmpty()) {
                        Comment comment = allComments.get(0);
                        activity.setComment(comment);
                        Review review = reviewDAO.findById(comment.getReviewId());
                        if (review != null) activity.setReview(review);
                    }
                } catch (SQLException ignored) {}
            } else if ("favorite".equals(type) && referenceText != null) {
                try {
                    String itemTypeQuery = "SELECT type FROM spotify_items WHERE id = ?;";
                    try (PreparedStatement itemStmt = DatabaseManager.getConnection().prepareStatement(itemTypeQuery)) {
                        itemStmt.setString(1, referenceText);
                        try (ResultSet itemRs = itemStmt.executeQuery()) {
                            if (itemRs.next()) {
                                String itemType = itemRs.getString("type");
                                Item item = SpotifyItemDAO.findById(referenceText, itemType);
                                activity.setItem(item);
                            }
                        }
                    }
                } catch (SQLException ignored) {}
            }
            activities.add(activity);
        }
        stmt.close();
        return activities;
    }

    public List<Activity> findNotificationsForUser(int userId) throws SQLException {
        String query = "SELECT a.type, a.reference_id, a.reference_text, a.created_at, " +
                "u.id AS u_id, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM activities a JOIN users u ON a.user_id = u.id " +
                "WHERE a.user_id != ? AND (" +
                "  (a.type = 'follow' AND a.reference_id = ?)" +
                "  OR" +
                "  (a.type = 'like' AND a.reference_id IN (SELECT id FROM reviews WHERE user_id = ?))" +
                "  OR" +
                "  (a.type = 'comment' AND a.reference_id IN (SELECT c.id FROM comments c JOIN reviews r ON c.review_id = r.id WHERE r.user_id = ?))" +
                ") " +
                "ORDER BY a.created_at DESC LIMIT 50;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setInt(2, userId);
        stmt.setInt(3, userId);
        stmt.setInt(4, userId);
        ResultSet rs = stmt.executeQuery();

        List<Activity> notifications = new ArrayList<>();
        ReviewDAO reviewDAO = new ReviewDAO();
        CommentDAO commentDAO = new CommentDAO();
        while (rs.next()) {
            User user = new User(
                rs.getInt("u_id"),
                rs.getString("email"),
                "",
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("bio"),
                rs.getString("profile_image_url")
            );
            String type = rs.getString("type");
            int referenceId = rs.getInt("reference_id");
            String referenceText = rs.getString("reference_text");
            String createdAt = rs.getString("created_at");

            Activity activity = new Activity(0, user, type, createdAt);
            activity.setReferenceText(referenceText);
            if ("review".equals(type)) {
                try {
                    Review review = reviewDAO.findById(referenceId);
                    if (review != null) activity.setReview(review);
                } catch (SQLException ignored) {}
            } else if ("like".equals(type)) {
                try {
                    Review review = reviewDAO.findById(referenceId);
                    if (review != null) activity.setReview(review);
                } catch (SQLException ignored) {}
            } else if ("comment".equals(type)) {
                try {
                    List<Comment> allComments = commentDAO.findByCommentId(referenceId);
                    if (!allComments.isEmpty()) {
                        Comment comment = allComments.get(0);
                        activity.setComment(comment);
                        Review review = reviewDAO.findById(comment.getReviewId());
                        if (review != null) activity.setReview(review);
                    }
                } catch (SQLException ignored) {}
            }
            notifications.add(activity);
        }
        stmt.close();
        return notifications;
    }

    public boolean hasUnreadNotifications(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM activities a " +
                "WHERE a.user_id != ? AND (" +
                "  (a.type = 'follow' AND a.reference_id = ?)" +
                "  OR" +
                "  (a.type = 'like' AND a.reference_id IN (SELECT id FROM reviews WHERE user_id = ?))" +
                "  OR" +
                "  (a.type = 'comment' AND a.reference_id IN (SELECT c.id FROM comments c JOIN reviews r ON c.review_id = r.id WHERE r.user_id = ?))" +
                ") AND a.created_at > COALESCE((SELECT last_read_at FROM notification_settings WHERE user_id = ?), '1970-01-01 00:00:00');";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setInt(2, userId);
        stmt.setInt(3, userId);
        stmt.setInt(4, userId);
        stmt.setInt(5, userId);
        ResultSet rs = stmt.executeQuery();
        boolean hasUnread = false;
        if (rs.next()) {
            hasUnread = rs.getInt(1) > 0;
        }
        stmt.close();
        return hasUnread;
    }

    public void markNotificationsAsRead(int userId) throws SQLException {
        String query = "INSERT OR REPLACE INTO notification_settings (user_id, last_read_at) VALUES (?, datetime('now'));";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.execute();
        stmt.close();
    }
}
