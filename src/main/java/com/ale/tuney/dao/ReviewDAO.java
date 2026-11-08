package com.ale.tuney.dao;

import com.ale.tuney.model.Item;
import com.ale.tuney.model.Review;
import com.ale.tuney.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public int save(Review review) throws SQLException {
        SpotifyItemDAO.save(review.getTarget());

        String query = "INSERT INTO reviews (user_id, spotify_id, type, rating, text) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(
                query, Statement.RETURN_GENERATED_KEYS
        );
        stmt.setInt(1, review.getAuthor().getId());
        stmt.setString(2, review.getTarget().getId());
        stmt.setString(3, review.getTarget().getType());
        stmt.setInt(4, review.getRating());
        stmt.setString(5, review.getText());
        stmt.execute();

        ResultSet keys = stmt.getGeneratedKeys();
        int generatedId = keys.next() ? keys.getInt(1) : 0;
        stmt.close();
        return generatedId;
    }

    public Review findById(int id) throws SQLException {
        String query = "SELECT r.*, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM reviews r JOIN users u ON r.user_id = u.id " +
                "WHERE r.id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, id);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return buildReview(rs);
        }
        return null;
    }

    public List<Review> findByUserIdSorted(int userId, String sortBy) throws SQLException {
        String orderByClause = switch (sortBy) {
            case "Voto decrescente" -> "r.rating DESC, r.created_at DESC";
            case "Voto crescente" -> "r.rating ASC, r.created_at DESC";
            case "Meno recenti" -> "r.created_at ASC";
            default -> "r.created_at DESC";
        };
        String query = "SELECT r.*, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM reviews r JOIN users u ON r.user_id = u.id " +
                "WHERE r.user_id = ? ORDER BY " + orderByClause + ";";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        return buildReviewList(stmt.executeQuery());
    }

    public List<Review> findAll() throws SQLException {
        String query = "SELECT r.*, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM reviews r JOIN users u ON r.user_id = u.id " +
                "ORDER BY r.created_at DESC;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        return buildReviewList(stmt.executeQuery());
    }

    public List<Review> findByFollowed(int userId) throws SQLException {
        String query = "SELECT r.*, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM reviews r JOIN users u ON r.user_id = u.id " +
                "WHERE r.user_id IN (SELECT followed_id FROM follows WHERE follower_id = ?) " +
                "ORDER BY r.created_at DESC;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        return buildReviewList(stmt.executeQuery());
    }

    public List<Review> findBySpotifyItemId(String spotifyId) throws SQLException {
        String query = "SELECT r.*, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM reviews r JOIN users u ON r.user_id = u.id " +
                "WHERE r.spotify_id = ? ORDER BY r.created_at DESC;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setString(1, spotifyId);
        return buildReviewList(stmt.executeQuery());
    }

    public void delete(int id) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // cancella attività commenti
            try (PreparedStatement stmtCommentAct = conn.prepareStatement(
                    "DELETE FROM activities WHERE type = 'comment' AND reference_id IN (SELECT id FROM comments WHERE review_id = ?);")) {
                stmtCommentAct.setInt(1, id);
                stmtCommentAct.execute();
            }

            // cancella commenti
            try (PreparedStatement stmtComments = conn.prepareStatement("DELETE FROM comments WHERE review_id = ?;")) {
                stmtComments.setInt(1, id);
                stmtComments.execute();
            }

            // cancella attività like
            try (PreparedStatement stmtLikeAct = conn.prepareStatement(
                    "DELETE FROM activities WHERE type = 'like' AND reference_id = ?;")) {
                stmtLikeAct.setInt(1, id);
                stmtLikeAct.execute();
            }

            // cancella like
            try (PreparedStatement stmtLikes = conn.prepareStatement("DELETE FROM likes WHERE review_id = ?;")) {
                stmtLikes.setInt(1, id);
                stmtLikes.execute();
            }

            // cancella attività review
            try (PreparedStatement stmtAct = conn.prepareStatement("DELETE FROM activities WHERE type = 'review' AND reference_id = ?;")) {
                stmtAct.setInt(1, id);
                stmtAct.execute();
            }

            // cancella review
            try (PreparedStatement stmtReview = conn.prepareStatement("DELETE FROM reviews WHERE id = ?;")) {
                stmtReview.setInt(1, id);
                stmtReview.execute();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    // aggiorna voto e testo di una review esistente
    public void update(int id, int rating, String text) throws SQLException {
        String query = "UPDATE reviews SET rating = ?, text = ? WHERE id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, rating);
        stmt.setString(2, text);
        stmt.setInt(3, id);
        stmt.execute();
        stmt.close();
    }

    // controlla se l'utente ha già recensito un item
    public boolean existsByUserAndItem(int userId, String spotifyId) throws SQLException {
        String query = "SELECT COUNT(*) FROM reviews WHERE user_id = ? AND spotify_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setString(2, spotifyId);
        ResultSet rs = stmt.executeQuery();
        boolean exists = rs.next() && rs.getInt(1) > 0;
        stmt.close();
        return exists;
    }

    public Review findByUserAndItem(int userId, String spotifyId) throws SQLException {
        String query = "SELECT r.*, u.email, u.username, u.name, u.bio, u.profile_image_url " +
                "FROM reviews r JOIN users u ON r.user_id = u.id " +
                "WHERE r.user_id = ? AND r.spotify_id = ?;";
        PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setString(2, spotifyId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return buildReview(rs);
        }
        return null;
    }

    private Review buildReview(ResultSet rs) throws SQLException {
        User author = new User(
                rs.getInt("user_id"),
                rs.getString("email"),
                "",
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("bio"),
                rs.getString("profile_image_url")
        );

        String spotifyId = rs.getString("spotify_id");
        String type = rs.getString("type");

        Item target = SpotifyItemDAO.findById(spotifyId, type);

        return new Review(
                rs.getInt("id"),
                target,
                author,
                rs.getInt("rating"),
                rs.getString("text"),
                rs.getString("created_at")
        );
    }

    private List<Review> buildReviewList(ResultSet rs) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        while (rs.next()) {
            reviews.add(buildReview(rs));
        }
        return reviews;
    }
}
