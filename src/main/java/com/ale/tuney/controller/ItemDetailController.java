package com.ale.tuney.controller;

import com.ale.tuney.ThemeManager;
import com.ale.tuney.api.SpotifyClient;
import com.ale.tuney.dao.ReviewDAO;
import com.ale.tuney.model.Album;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.Review;
import com.ale.tuney.model.Song;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.shape.SVGPath;
import com.ale.tuney.SessionManager;

import java.util.List;

public class ItemDetailController {

    @FXML
    private ImageView cover;
    @FXML
    private Label titleLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Label yearLabel;
    @FXML
    private Label typeLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private VBox reviewsContainer;
    @FXML
    private Label noReviewLabel;
    @FXML
    private javafx.scene.control.Button saveItemLabel;
    @FXML
    private VBox tracklistSection;
    @FXML
    private ProgressIndicator tracklistProgress;
    @FXML
    private VBox tracklistContainer;
    @FXML
    private Separator tracklistSeparator;
    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox reviewsStatsBox;
    @FXML
    private VBox statsDataBox;
    @FXML
    private VBox emptyStatsBox;
    @FXML
    private Label emptyStatsLabel;
    @FXML
    private Label avgRatingLabel;
    @FXML
    private Label totalReviewsSublabel;
    @FXML
    private ProgressBar bar1, bar2, bar3, bar4, bar5;
    @FXML
    private Label count1, count2, count3, count4, count5;

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private Item currentItem;
    private Node previousCenterNode;

    public void setPreviousCenterNode(Node node) {
        this.previousCenterNode = node;
    }

    @FXML
    public void initialize() {
        noReviewLabel.setVisible(false);
        noReviewLabel.setManaged(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        Platform.runLater(() -> {
            if (cover.getScene() != null) {
                ThemeManager.applyTheme(cover.getScene().getRoot());
            }
        });
    }

    public void setItem(Item item) {
        this.currentItem = item;

        if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
            cover.setImage(com.ale.tuney.ImageLoader.load(item.getImageUrl()));
        }

        titleLabel.setText(item.getTitle());
        artistLabel.setText(String.join(", ", item.getArtists()));

        boolean isAlbum = item instanceof Album;
        typeLabel.setText(isAlbum ? "Album" : "Brano");
        
        SVGPath svgType = new SVGPath();
        if (isAlbum) {
            svgType.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z");
        } else {
            svgType.setContent("M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z");
        }
        svgType.setFill(Color.web("#9b6fd4"));
        svgType.setScaleX(0.65);
        svgType.setScaleY(0.65);
        typeLabel.setGraphic(svgType);

        if (item instanceof Album album && album.getYear() > 0) {
            yearLabel.setText(String.valueOf(album.getYear()));
            yearLabel.setVisible(true);
            yearLabel.setManaged(true);
        } else {
            yearLabel.setVisible(false);
            yearLabel.setManaged(false);
        }

        updateSaveStatus();
        if (rootPane != null && rootPane.getCenter() != null) {
            rootPane.getCenter().getProperties().put("itemDetailController", this);
        }
        loadReviews();

        if (item instanceof Album) {
            loadTracklist((Album) item);
        }
    }

    public void loadReviews() {
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        Task<List<Review>> task = new Task<>() {
            @Override
            protected List<Review> call() throws Exception {
                return reviewDAO.findBySpotifyItemId(currentItem.getId());
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            List<Review> reviews = task.getValue();
            Platform.runLater(() -> showReviews(reviews));
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadTracklist(Album album) {
        tracklistSection.setVisible(true);
        tracklistSection.setManaged(true);
        tracklistSeparator.setVisible(true);
        tracklistSeparator.setManaged(true);
        tracklistProgress.setVisible(true);
        tracklistProgress.setManaged(true);

        Task<List<Song>> task = new Task<>() {
            @Override
            protected List<Song> call() {
                return SpotifyClient.getAlbumTracks(album.getId());
            }
        };

        task.setOnSucceeded(e -> {
            tracklistProgress.setVisible(false);
            tracklistProgress.setManaged(false);
            List<Song> tracks = task.getValue();
            Platform.runLater(() -> showTracklist(tracks, album));
        });

        task.setOnFailed(e -> {
            tracklistProgress.setVisible(false);
            tracklistProgress.setManaged(false);
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showTracklist(List<Song> tracks, Album album) {
        tracklistContainer.getChildren().clear();
        for (int i = 0; i < tracks.size(); i++) {
            Song song = tracks.get(i);
            Song songConCover = song.getImageUrl().isBlank()
                    ? new Song(song.getId(), song.getTitle(), song.getArtists(), album.getImageUrl())
                    : song;

            int trackNum = i + 1;
            HBox row = createTrackRow(trackNum, songConCover);
            tracklistContainer.getChildren().add(row);

            if (i < tracks.size() - 1) {
                Separator sep = new Separator();
                sep.setOpacity(0.08);
                tracklistContainer.getChildren().add(sep);
            }
        }
    }

    private HBox createTrackRow(int numero, Song song) {
        Label numLabel = new Label(String.valueOf(numero));
        numLabel.setTextFill(Color.web("#666"));
        numLabel.setFont(new Font(13));
        numLabel.setMinWidth(32);
        numLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label title = new Label(song.getTitle());
        title.setTextFill(Color.WHITE);
        title.setFont(new Font(14));

        Label artist = new Label(String.join(", ", song.getArtists()));
        artist.setTextFill(Color.web("#888"));
        artist.setFont(new Font(12));

        VBox text = new VBox(2, title, artist);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(16, numLabel, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 8, 10, 8));
        row.setStyle("-fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"));

        row.setOnMouseClicked(e -> {
            try {
                BorderPane mainRoot = (BorderPane) row.getScene().getRoot();
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/ale/tuney/item-detail-view.fxml"));
                BorderPane loadedRoot = loader.load();
                ItemDetailController ctrl = loader.getController();
                ctrl.setItem(song);
                ctrl.setPreviousCenterNode(mainRoot.getCenter());
                mainRoot.setCenter(loadedRoot.getCenter());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return row;
    }

    private void showReviews(List<Review> reviews) {
        reviewsContainer.getChildren().clear();

        updateReviewsStats(reviews);

        if (reviews.isEmpty()) {
            noReviewLabel.setVisible(true);
            noReviewLabel.setManaged(true);
            return;
        }

        noReviewLabel.setVisible(false);
        noReviewLabel.setManaged(false);

        FlowPane fp = new FlowPane();
        fp.setHgap(16);
        fp.setVgap(16);
        fp.setAlignment(Pos.TOP_LEFT);

        for (Review review : reviews) {
            fp.getChildren().add(createReviewCard(review));
        }
        reviewsContainer.getChildren().add(fp);
    }

    private void updateReviewsStats(List<Review> reviews) {
        if (reviewsStatsBox == null || statsDataBox == null || emptyStatsBox == null) {
            return;
        }

        boolean hasReviews = reviews != null && !reviews.isEmpty();

        if (!hasReviews) {
            statsDataBox.setVisible(false);
            statsDataBox.setManaged(false);
            if (emptyStatsLabel != null) {
                emptyStatsLabel.setText("Non è possibile calcolare le statistiche per questo elemento.");
            }
            emptyStatsBox.setVisible(true);
            emptyStatsBox.setManaged(true);
            return;
        }

        emptyStatsBox.setVisible(false);
        emptyStatsBox.setManaged(false);
        statsDataBox.setVisible(true);
        statsDataBox.setManaged(true);

        int totalReviews = reviews.size();
        int[] counts = new int[5];
        double sum = 0.0;
        for (Review r : reviews) {
            int rating = r.getRating();
            if (rating >= 1 && rating <= 5) {
                counts[rating - 1]++;
                sum += rating;
            }
        }

        double average = sum / totalReviews;
        if (avgRatingLabel != null) {
            avgRatingLabel.setText(String.format(java.util.Locale.US, "%.1f", average));
        }
        if (totalReviewsSublabel != null) {
            totalReviewsSublabel.setText(totalReviews == 1 ? "media su 1 recensione" : String.format("media su %d recensioni", totalReviews));
        }

        ProgressBar[] progressBars = {bar1, bar2, bar3, bar4, bar5};
        Label[] countLabels = {count1, count2, count3, count4, count5};

        for (int i = 0; i < 5; i++) {
            int count = counts[i];
            double progress = (double) count / totalReviews;
            if (progressBars[i] != null) {
                progressBars[i].setProgress(progress);
            }
            if (countLabels[i] != null) {
                countLabels[i].setText(String.valueOf(count));
            }
        }
    }

    private VBox createReviewCard(Review review) {
        VBox card = new VBox(8);
        card.setPrefWidth(280);
        card.setPrefHeight(180);
        card.setMinHeight(180);
        card.setMaxHeight(180);
        card.setStyle("-fx-background-color: #0d0d0d; -fx-background-radius: 16; -fx-cursor: hand;");
        card.setPadding(new Insets(12));

        Label username = new Label("@" + review.getAuthor().getUsername());
        username.setTextFill(Color.web("#ffb3d9"));
        username.setFont(new Font(16));
        username.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");

        javafx.scene.shape.Circle avatarBg = new javafx.scene.shape.Circle(16, Color.web("#3a3a3a"));
        ImageView avatar = new ImageView();
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(16, 16, 16);
        avatar.setClip(clip);
        javafx.scene.image.Image profilePic =
                com.ale.tuney.ImageLoader.loadProfilePicture(review.getAuthor().getProfileImageUrl());
        com.ale.tuney.ImageLoader.applyProfilePicture(avatar, profilePic, 32);
        javafx.scene.layout.StackPane avatarStack = new javafx.scene.layout.StackPane(avatarBg, avatar);
        avatarStack.setPrefSize(32, 32);
        avatarStack.setMinSize(32, 32);
        avatarStack.setMaxSize(32, 32);

        HBox userRow = new HBox(8, avatarStack, username);
        userRow.setAlignment(Pos.CENTER_LEFT);
        userRow.setStyle("-fx-cursor: hand;");
        userRow.setOnMouseClicked(e -> {
            e.consume();
            openUserProfile(review.getAuthor(), username);
        });

        String stars = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
        Label starsLabel = new Label(stars);
        starsLabel.setTextFill(Color.web("#ffe11f"));
        starsLabel.setFont(new Font(20));

        Label text = new Label();
        text.setTextFill(Color.LIGHTGRAY);
        text.setFont(new Font(14));
        text.setWrapText(true);
        text.setMaxWidth(256);
        text.setText(truncateText(review.getText(), 256, text.getFont(), 3));

        Label data = new Label(review.getCreatedAt() != null
                ? review.getCreatedAt().substring(0, Math.min(10, review.getCreatedAt().length()))
                : "");
        data.setTextFill(Color.web("#555"));
        data.setFont(new Font(12));

        card.getChildren().addAll(userRow, starsLabel, text, data);

        card.setOnMouseEntered(ev -> card.setStyle(
                "-fx-background-color: #1a1a1a; -fx-background-radius: 16; -fx-cursor: hand;"));
        card.setOnMouseExited(ev -> card.setStyle(
                "-fx-background-color: #0d0d0d; -fx-background-radius: 16; -fx-cursor: hand;"));

        card.setOnMouseClicked(ev -> openReviewDetail(review, card));

        return card;
    }

    private String truncateText(String text, double maxWidth, Font font, int maxLines) {
        if (text == null || text.isBlank()) {
            return "";
        }
        javafx.scene.text.Text measurer = new javafx.scene.text.Text();
        measurer.setFont(font);
        measurer.setWrappingWidth(maxWidth);

        javafx.scene.text.Text singleLine = new javafx.scene.text.Text("Ag");
        singleLine.setFont(font);
        double lineHeight = singleLine.getLayoutBounds().getHeight();
        double maxHeight = lineHeight * maxLines + 2;

        measurer.setText(text);
        if (measurer.getLayoutBounds().getHeight() <= maxHeight) {
            return text;
        }

        String current = text;
        while (!current.isEmpty()) {
            current = current.substring(0, current.length() - 1);
            measurer.setText(current.stripTrailing() + "…");
            if (measurer.getLayoutBounds().getHeight() <= maxHeight) {
                return current.stripTrailing() + "…";
            }
        }
        return text;
    }

    private void openUserProfile(com.ale.tuney.model.User user, Node source) {
        try {
            BorderPane mainRoot = (BorderPane) source.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ale/tuney/profile-view.fxml"));
            BorderPane profileView = loader.load();
            ProfileController ctrl = loader.getController();
            ctrl.setUser(user);
            mainRoot.setCenter(profileView.getCenter());

            MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
            if (mainCtrl != null) {
                mainCtrl.setActiveMenu("Profilo");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openReviewDetail(Review review, javafx.scene.Node source) {
        try {
            BorderPane mainRoot = (BorderPane) source.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewDetailController ctrl = loader.getController();
            ctrl.setReview(review);
            ctrl.setPreviousCenterNode(mainRoot.getCenter());
            mainRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        if (previousCenterNode != null) {
            BorderPane mainRoot = (BorderPane) saveItemLabel.getScene().getRoot();
            mainRoot.setCenter(previousCenterNode);
        }
    }

    @FXML
    private void writeReview() {
        if (currentItem == null)
            return;
        com.ale.tuney.model.User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            try {
                if (reviewDAO.existsByUserAndItem(currentUser.getId(), currentItem.getId())) {
                    com.ale.tuney.model.Review existing = reviewDAO.findByUserAndItem(currentUser.getId(),
                            currentItem.getId());
                    if (existing != null) {
                        openEditReview(existing);
                        return;
                    }
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
        try {
            BorderPane mainRoot = (BorderPane) saveItemLabel.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/review-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewController ctrl = loader.getController();
            ctrl.setItem(currentItem);
            ctrl.setPreviousCenterNode(mainRoot.getCenter());
            mainRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditReview(com.ale.tuney.model.Review review) {
        try {
            BorderPane mainRoot = (BorderPane) saveItemLabel.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewDetailController ctrl = loader.getController();
            ctrl.setReviewAndEdit(review);
            ctrl.setPreviousCenterNode(mainRoot.getCenter());
            mainRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSaveStatus() {
        if (currentItem == null)
            return;
        com.ale.tuney.model.User user = SessionManager.getCurrentUser();
        if (user == null)
            return;
        com.ale.tuney.dao.SavedItemDAO savedItemDAO = new com.ale.tuney.dao.SavedItemDAO();
        try {
            SVGPath svg = new SVGPath();
            svg.setFill(Color.WHITE);
            saveItemLabel.getStyleClass().removeAll("btn-save-circle", "btn-save-circle-saved");
            saveItemLabel.setStyle("");
            if (savedItemDAO.isSaved(user.getId(), currentItem.getId())) {
                svg.setContent("M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z");
                saveItemLabel.setGraphic(svg);
                saveItemLabel.setText("");
                saveItemLabel.getStyleClass().add("btn-save-circle-saved");
            } else {
                svg.setContent("M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15-5-2.18-5 2.18V5h10v13z");
                saveItemLabel.setGraphic(svg);
                saveItemLabel.setText("");
                saveItemLabel.getStyleClass().add("btn-save-circle");
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleSaveItem() {
        if (currentItem == null)
            return;
        com.ale.tuney.model.User user = SessionManager.getCurrentUser();
        if (user == null)
            return;
        com.ale.tuney.dao.SavedItemDAO savedItemDAO = new com.ale.tuney.dao.SavedItemDAO();
        try {
            if (savedItemDAO.isSaved(user.getId(), currentItem.getId())) {
                savedItemDAO.unsave(user.getId(), currentItem.getId());
            } else {
                savedItemDAO.save(user.getId(), currentItem);
            }
            updateSaveStatus();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }
}
