package com.ale.tuney.api;

import com.ale.tuney.model.Album;
import com.ale.tuney.model.Song;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SpotifyClient {

    private static final String CLIENT_ID;
    private static final String CLIENT_SECRET;
    private static final boolean CONFIGURED;

    static {
        Properties props = new Properties();
        File configFile = new File("config.properties");
        String id = null;
        String secret = null;

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                id = props.getProperty("spotify.client.id");
                secret = props.getProperty("spotify.client.secret");
            } catch (IOException e) {
                System.err.println("[SpotifyClient] Errore nella lettura di config.properties: " + e.getMessage());
            }
        }

        // Fallback su variabili d'ambiente
        if (id == null || id.isBlank() || id.contains("YOUR_CLIENT_ID")) {
            id = System.getenv("SPOTIFY_CLIENT_ID");
        }
        if (secret == null || secret.isBlank() || secret.contains("YOUR_CLIENT_SECRET")) {
            secret = System.getenv("SPOTIFY_CLIENT_SECRET");
        }

        if (id != null && !id.isBlank() && !id.contains("YOUR_CLIENT_ID")
                && secret != null && !secret.isBlank() && !secret.contains("YOUR_CLIENT_SECRET")) {
            CLIENT_ID = id.trim();
            CLIENT_SECRET = secret.trim();
            CONFIGURED = true;
        } else {
            CLIENT_ID = null;
            CLIENT_SECRET = null;
            CONFIGURED = false;
            System.err.println("[SpotifyClient] Credenziali Spotify non configurate (config.properties mancante o non compilato).");
            System.err.println("[SpotifyClient] L'app funzionerà normalmente con i dati locali del database. La ricerca online su Spotify è disabilitata.");
        }
    }

    private static String accessToken = null;
    private static long tokenExpiresAt = 0;

    public static boolean isConfigured() {
        return CONFIGURED;
    }

    private static void authenticate() {
        if (!CONFIGURED) return;

        try {
            HttpResponse<String> response = Unirest.post("https://accounts.spotify.com/api/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .basicAuth(CLIENT_ID, CLIENT_SECRET)
                    .field("grant_type", "client_credentials")
                    .asString();

            if (response.getStatus() != 200) {
                System.err.println("[SpotifyClient] Autenticazione fallita (HTTP " + response.getStatus() + "): " + response.getBody());
                accessToken = null;
                return;
            }

            JSONObject body = new JSONObject(response.getBody());
            accessToken = body.getString("access_token");
            int expiresIn = body.getInt("expires_in");
            tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
        } catch (Exception e) {
            System.err.println("[SpotifyClient] Errore durante l'autenticazione: " + e.getMessage());
            accessToken = null;
        }
    }

    private static void ensureAuthenticated() {
        if (!CONFIGURED) return;
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
            authenticate();
        }
    }

    public static List<Object> search(String query) {
        if (!CONFIGURED) {
            return new ArrayList<>();
        }

        ensureAuthenticated();
        if (accessToken == null) {
            return new ArrayList<>();
        }

        HttpResponse<String> response = Unirest.get("https://api.spotify.com/v1/search")
                .header("Authorization", "Bearer " + accessToken)
                .queryString("q", query)
                .queryString("type", "track,album")
                .queryString("limit", "10")
                .asString();

        String rawBody = response.getBody();

        if (rawBody == null || !rawBody.trim().startsWith("{")) {
            System.err.println("Risposta Spotify non valida in search(): " + rawBody);
            return new ArrayList<>();
        }

        JSONObject body = new JSONObject(rawBody);

        if (body.has("error")) {
            JSONObject error = body.getJSONObject("error");
            System.err.println("Errore Spotify: " + error);
            if (error.optInt("status") == 401) {
                accessToken = null;
            }
            return new ArrayList<>();
        }

        JSONArray tracks = body.getJSONObject("tracks").getJSONArray("items");
        JSONArray albums = body.getJSONObject("albums").getJSONArray("items");

        List<Object> results = new ArrayList<>();
        int maxLength = Math.max(tracks.length(), albums.length());

        for (int i = 0; i < maxLength; i++) {
            if (i < tracks.length()) {
                results.add(parseSong(tracks.getJSONObject(i)));
            }
            if (i < albums.length()) {
                results.add(parseAlbum(albums.getJSONObject(i)));
            }
        }

        return results;
    }

    private static String getCoverUrl(JSONArray images) {
        if (images == null || images.isEmpty()) {
            return "";
        }
        if (images.length() > 1) {
            return images.getJSONObject(1).getString("url");
        }
        return images.getJSONObject(0).getString("url");
    }

    private static Song parseSong(JSONObject track) {
        String id = track.getString("id");
        String title = track.getString("name");
        List<String> artists = parseArtists(track.getJSONArray("artists"));
        String imageUrl = getCoverUrl(track.getJSONObject("album").getJSONArray("images"));
        return new Song(id, title, artists, imageUrl);
    }

    private static Album parseAlbum(JSONObject albumJson) {
        String id = albumJson.getString("id");
        String title = albumJson.getString("name");
        int year = Integer.parseInt(albumJson.getString("release_date").substring(0, 4));
        List<String> artists = parseArtists(albumJson.getJSONArray("artists"));
        String imageUrl = getCoverUrl(albumJson.getJSONArray("images"));
        return new Album(id, title, artists, year, imageUrl);
    }

    private static List<String> parseArtists(JSONArray artistsJson) {
        List<String> artists = new ArrayList<>();
        for (int i = 0; i < artistsJson.length(); i++) {
            artists.add(artistsJson.getJSONObject(i).getString("name"));
        }
        return artists;
    }

    public static List<Song> getAlbumTracks(String albumId) {
        if (!CONFIGURED) {
            return new ArrayList<>();
        }

        ensureAuthenticated();
        if (accessToken == null) {
            return new ArrayList<>();
        }

        HttpResponse<String> response = Unirest
                .get("https://api.spotify.com/v1/albums/" + albumId + "/tracks")
                .header("Authorization", "Bearer " + accessToken)
                .queryString("limit", "50")
                .asString();

        String body = response.getBody();
        if (body == null || !body.trim().startsWith("{")) {
            System.err.println("Risposta Spotify non valida in getAlbumTracks(): " + body);
            return new ArrayList<>();
        }

        JSONObject json = new JSONObject(body);
        if (json.has("error")) {
            JSONObject error = json.getJSONObject("error");
            System.err.println("Errore Spotify tracklist: " + error);
            if (error.optInt("status") == 401) {
                accessToken = null;
            }
            return new ArrayList<>();
        }
        JSONArray items = json.getJSONArray("items");
        List<Song> tracks = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject t = items.getJSONObject(i);
            String id = t.getString("id");
            String title = t.getString("name");
            List<String> artists = parseArtists(t.getJSONArray("artists"));
            tracks.add(new Song(id, title, artists, ""));
        }
        return tracks;
    }
}
