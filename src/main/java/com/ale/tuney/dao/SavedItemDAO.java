package com.ale.tuney.dao;

import com.ale.tuney.model.Album;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.Song;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SavedItemDAO {

    public void save(int userId, Item item) throws SQLException {
        SpotifyItemDAO.save(item);

        String query = "INSERT OR IGNORE INTO saved_items (user_id, item_id, timestamp) VALUES (?, ?, CURRENT_TIMESTAMP);";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, item.getId());
            stmt.execute();
        }
    }

    public void unsave(int userId, String itemId) throws SQLException {
        String query = "DELETE FROM saved_items WHERE user_id = ? AND item_id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, itemId);
            stmt.execute();
        }
    }

    public boolean isSaved(int userId, String itemId) throws SQLException {
        String query = "SELECT 1 FROM saved_items WHERE user_id = ? AND item_id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Item> getSavedItems(int userId) throws SQLException {
        String query = "SELECT s.*, i.type, i.title, i.artists, i.image_url, i.image_base64, i.year " +
                       "FROM saved_items s " +
                       "JOIN spotify_items i ON s.item_id = i.id " +
                       "WHERE s.user_id = ? " +
                       "ORDER BY s.timestamp DESC;";
        List<Item> items = new ArrayList<>();
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("item_id");
                    String type = rs.getString("type");
                    String title = rs.getString("title");
                    String artistsStr = rs.getString("artists");
                    List<String> artists = List.of(artistsStr.split(", "));
                    String base64Image = rs.getString("image_base64");
                    String imageUrl = (base64Image != null && !base64Image.isBlank()) ? base64Image : rs.getString("image_url");

                    if ("song".equalsIgnoreCase(type)) {
                        items.add(new Song(id, title, artists, imageUrl));
                    } else {
                        int year = rs.getInt("year");
                        items.add(new Album(id, title, artists, year, imageUrl));
                    }
                }
            }
        }
        return items;
    }

    public int getSaveCount(String itemId) throws SQLException {
        String query = "SELECT COUNT(*) FROM saved_items WHERE item_id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
