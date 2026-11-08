package com.ale.tuney;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import com.ale.tuney.model.User;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ImageLoader {
    public static Image load(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        if (imageUrl.startsWith("data:")) {
            try {
                int commaIndex = imageUrl.indexOf(',');
                if (commaIndex != -1) {
                    String base64Data = imageUrl.substring(commaIndex + 1);
                    byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
                    return new Image(new ByteArrayInputStream(decodedBytes));
                }
            } catch (Exception e) {
                System.err.println("Errore nel caricamento dell'immagine Base64: " + e.getMessage());
                return null;
            }
        }
        return new Image(imageUrl, true);
    }

    public static Image loadProfilePicture(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || imageUrl.equalsIgnoreCase("null")) {
            try {
                return new Image(ImageLoader.class.getResourceAsStream("/images/default-avatar.png"));
            } catch (Exception e) {
                System.err.println("Errore nel caricamento del default-avatar.png: " + e.getMessage());
                return null;
            }
        }
        if (imageUrl.startsWith("file:")) {
            try {
                String path = imageUrl;
                if (path.startsWith("file:///")) {
                    path = path.substring(8);
                } else if (path.startsWith("file:/")) {
                    path = path.substring(6);
                }
                path = java.net.URLDecoder.decode(path, "UTF-8");
                java.io.File file = new java.io.File(path);
                if (!file.exists() || !file.isFile()) {

                    return new Image(ImageLoader.class.getResourceAsStream("/images/default-avatar.png"));
                }
            } catch (Exception e) {
                System.err.println("Errore nel controllo del file locale: " + e.getMessage());
            }
        }
        Image img = load(imageUrl);
        if (img == null || img.isError()) {
            try {
                return new Image(ImageLoader.class.getResourceAsStream("/images/default-avatar.png"));
            } catch (Exception e) {
                System.err.println("Errore nel caricamento del default-avatar.png: " + e.getMessage());
                return null;
            }
        }
        return img;
    }

    public static void loadNavbarImage(ImageView view) {
        if (view == null) return;

        Circle navClip = new Circle(20, 20, 20);
        view.setClip(navClip);

        User user = SessionManager.getCurrentUser();
        if (user != null) {
            Image img = loadProfilePicture(user.getProfileImageUrl());
            if (img != null) {
                if (img.getProgress() == 1.0) {
                    applyCoverImage(view, img);
                } else {
                    img.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() == 1.0) {
                            applyCoverImage(view, img);
                        }
                    });
                }
            }
        }
    }

    public static void applyProfilePicture(ImageView view, Image img, double fitSize) {
        if (view == null || img == null) return;
        view.setFitWidth(fitSize);
        view.setFitHeight(fitSize);
        view.setPreserveRatio(false);

        if (img.getProgress() == 1.0 && img.getWidth() > 0) {
            applySquareViewport(view, img);
        } else {
            view.setImage(img);
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0) {
                    applySquareViewport(view, img);
                }
            });
        }
    }

    private static void applySquareViewport(ImageView view, Image img) {
        double w = img.getWidth();
        double h = img.getHeight();
        if (w <= 0 || h <= 0) return;
        double size = Math.min(w, h);
        view.setImage(img);
        view.setViewport(new Rectangle2D((w - size) / 2, (h - size) / 2, size, size));
    }

    private static void applyCoverImage(ImageView view, Image img) {
        double w = img.getWidth();
        double h = img.getHeight();
        if (w <= 0 || h <= 0)
            return;

        double size = Math.min(w, h);
        Rectangle2D viewport = new Rectangle2D(
                (w - size) / 2,
                (h - size) / 2,
                size,
                size);

        view.setImage(img);
        view.setViewport(viewport);
        view.setFitWidth(40);
        view.setFitHeight(40);
        view.setPreserveRatio(false);
    }
}
