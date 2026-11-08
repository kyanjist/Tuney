package com.ale.tuney.dao;

import com.ale.tuney.model.Comment;
import com.ale.tuney.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    public int save(Comment comment) throws SQLException {
        String query = "INSERT INTO comments (user_id, review_id, text) VALUES (?, ?, ?);";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, comment.getAuthor().getId());
        stmt.setInt(2, comment.getReviewId());
        stmt.setString(3, comment.getText());
        stmt.executeUpdate();
        ResultSet keys = stmt.getGeneratedKeys();
        int generatedId = -1;
        if (keys.next()) {
            generatedId = keys.getInt(1);
        }
        stmt.close();
        return generatedId;
    }

    public List<Comment> findByCommentId(int commentId) throws SQLException {
        String query = "SELECT c.*, u.email, u.password, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM comments c JOIN users u ON c.user_id = u.id " +
                "WHERE c.id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, commentId);
        ResultSet rs = stmt.executeQuery();

        List<Comment> comments = new ArrayList<>();
        if (rs.next()) {
            User author = new User(
                    rs.getInt("user_id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            );
            comments.add(new Comment(
                    rs.getInt("id"),
                    author,
                    rs.getInt("review_id"),
                    rs.getString("text"),
                    rs.getString("created_at")
            ));
        }
        stmt.close();
        return comments;
    }

    public List<Comment> findByReviewId(int reviewId) throws SQLException {
        String query = "SELECT c.*, u.email, u.password, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM comments c JOIN users u ON c.user_id = u.id " +
                "WHERE c.review_id = ? ORDER BY c.created_at ASC;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, reviewId);
        ResultSet rs = stmt.executeQuery();

        List<Comment> comments = new ArrayList<>();
        while (rs.next()) {
            User author = new User(
                    rs.getInt("user_id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("bio"),
                    rs.getString("profile_image_url")
            );
            comments.add(new Comment(
                    rs.getInt("id"),
                    author,
                    rs.getInt("review_id"),
                    rs.getString("text"),
                    rs.getString("created_at")
            ));
        }
        return comments;
    }

    public void delete(int id) throws SQLException {
        // rimuove l'attività associata al commento
        new ActivityDAO().deleteByTypeAndReferenceId("comment", id);

        String query = "DELETE FROM comments WHERE id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, id);
        stmt.execute();
        stmt.close();
    }
}
