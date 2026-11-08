package com.ale.tuney.dao;

import com.ale.tuney.model.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserFavoritesDAO {

    public List<Item> findFavoritesByUserId(int userId) throws SQLException {
        List<Item> favorites = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            favorites.add(null);
        }

        String query = "SELECT spotify_id, type, position FROM user_favorites WHERE user_id = ? ORDER BY position ASC;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String spotifyId = rs.getString("spotify_id");
                    String type = rs.getString("type");
                    int position = rs.getInt("position"); // da 1 a 4
                    
                    if (position >= 1 && position <= 4) {
                        Item item = SpotifyItemDAO.findById(spotifyId, type);
                        favorites.set(position - 1, item);
                    }
                }
            }
        }
        return favorites;
    }

    public void saveFavorites(int userId, List<Item> favorites) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // cancella preferiti esistenti
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM user_favorites WHERE user_id = ?;")) {
                deleteStmt.setInt(1, userId);
                deleteStmt.executeUpdate();
            }

            // inserisce nuovi preferiti
            String insertQuery = "INSERT INTO user_favorites (user_id, spotify_id, type, position) VALUES (?, ?, ?, ?);";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (int i = 0; i < favorites.size(); i++) {
                    Item item = favorites.get(i);
                    if (item != null) {
                        SpotifyItemDAO.save(item);

                        insertStmt.setInt(1, userId);
                        insertStmt.setString(2, item.getId());
                        insertStmt.setString(3, item.getType());
                        insertStmt.setInt(4, i + 1);
                        insertStmt.addBatch();
                    }
                }
                insertStmt.executeBatch();
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
