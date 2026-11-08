package com.ale.tuney.dao;

import com.ale.tuney.model.Album;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.Song;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.List;

public class SpotifyItemDAO {

    public static void save(Item item) {
        if (item == null || item.getId() == null) return;

        if (exists(item.getId())) {
            return;
        }

        String base64Image = downloadAndConvertToBase64(item.getImageUrl());

        String query = "INSERT OR IGNORE INTO spotify_items (id, type, title, artists, image_url, image_base64, year) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getType());
            stmt.setString(3, item.getTitle());
            stmt.setString(4, String.join(", ", item.getArtists()));
            stmt.setString(5, item.getImageUrl());
            stmt.setString(6, base64Image);

            if (item instanceof Album album) {
                stmt.setInt(7, album.getYear());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.execute();
        } catch (SQLException e) {
            System.err.println("Errore nel salvataggio dell'item in cache: " + e.getMessage());
        }
    }

    public static boolean exists(String id) {
        String query = "SELECT 1 FROM spotify_items WHERE id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static Item findById(String id, String type) {
        String query = "SELECT * FROM spotify_items WHERE id = ?;";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String artistsStr = rs.getString("artists");
                    List<String> artists = List.of(artistsStr.split(", "));
                    String base64Image = rs.getString("image_base64");
                    
                    // se l'immagine base64 è valida la usiamo direttamente, altrimenti URL originale
                    String imageUrl = (base64Image != null && !base64Image.isBlank()) ? base64Image : rs.getString("image_url");

                    if ("song".equalsIgnoreCase(type)) {
                        return new Song(id, title, artists, imageUrl);
                    } else {
                        int year = rs.getInt("year");
                        return new Album(id, title, artists, year, imageUrl);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel caricamento dell'item locale: " + e.getMessage());
        }

        // se l'item non è in cache restituiamo un placeholder locale per funzionare offline
        if ("song".equalsIgnoreCase(type)) {
            return new Song(id, "Dettagli non disponibili (Offline)", List.of("Sconosciuto"), null);
        } else {
            return new Album(id, "Dettagli non disponibili (Offline)", List.of("Sconosciuto"), 0, null);
        }
    }

    private static String downloadAndConvertToBase64(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        if (imageUrl.startsWith("data:")) {
            return imageUrl;
        }
        try {
            URL url = java.net.URI.create(imageUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    byte[] imageBytes = out.toByteArray();
                    String contentType = conn.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        contentType = "image/jpeg";
                    }
                    String base64Data = Base64.getEncoder().encodeToString(imageBytes);
                    return "data:" + contentType + ";base64," + base64Data;
                }
            }
        } catch (Exception e) {
            System.err.println("Impossibile scaricare o convertire la copertina in base64: " + e.getMessage());
        }
        return "";
    }
}
