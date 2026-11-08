package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.dao.ActivityDAO;
import com.ale.tuney.dao.FollowDAO;
import com.ale.tuney.dao.ReviewDAO;
import com.ale.tuney.model.Review;
import com.ale.tuney.model.User;
import com.ale.tuney.model.Activity;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.application.Platform;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.layout.FlowPane;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.Song;
import com.ale.tuney.dao.UserFavoritesDAO;

public class ProfileController implements Initializable {

    @FXML
    private ImageView profileImage;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label nomeLabel;
    @FXML
    private Label bioLabel;
    @FXML
    private Label reviewsCount;
    @FXML
    private Label followersCount;
    @FXML
    private Label followingCount;
    @FXML
    private Button followButton;
    @FXML
    private FlowPane reviewsPane;
    @FXML
    private ChoiceBox<String> reviewsSortOrder;
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
    @FXML
    private VBox favoritesSection;
    @FXML
    private HBox favoritesPane;
    @FXML
    private VBox emptyFavoritesBox;
    @FXML
    private Label emptyFavoritesLabel;
    @FXML
    private Button addFavoritesBtn;
    @FXML
    private VBox profileContent;
    @FXML
    private VBox profileCenterVBox;
    @FXML
    private VBox reviewsSection;
    @FXML
    private ScrollPane profileScrollPane;
    @FXML
    private HBox feedTab;
    @FXML
    private HBox attivitaTab;
    @FXML
    private Rectangle feedTabUnderline;
    @FXML
    private Rectangle activityTabUnderline;
    @FXML
    private VBox emptyReviewsBox;
    @FXML
    private Label emptyReviewsLabel;

    private User profileUser;
    private VBox activitySection;
    private boolean hasFavorites = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Circle clip = new Circle(80, 80, 80);
        profileImage.setClip(clip);

        if (profileCenterVBox != null) {
            profileCenterVBox.getProperties().put("profileController", this);
            profileCenterVBox.prefWidthProperty().bind(profileScrollPane.widthProperty());
        }

        profileScrollPane.layoutBoundsProperty().addListener((obs, old, bounds) -> {
            Rectangle rectClip = new Rectangle(bounds.getWidth(), bounds.getHeight());
            rectClip.setArcWidth(16);
            rectClip.setArcHeight(16);
            profileScrollPane.setClip(rectClip);
        });

        profileScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> adjustGridWidth());
        profileScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> adjustGridWidth());

        if (reviewsSortOrder != null) {
            reviewsSortOrder.getItems().addAll("Più recenti", "Meno recenti", "Voto decrescente", "Voto crescente");
            reviewsSortOrder.setValue("Più recenti");
            reviewsSortOrder.valueProperty().addListener((obs, oldVal, newVal) -> {
                caricaReviewsOrdinate(newVal);
            });
        }
    }

    public void setUser(User user) {
        this.profileUser = user;
        loadProfile();
    }

    public User getProfileUser() {
        return profileUser;
    }

    private void applyCoverImage(ImageView view, Image img) {
        if (img.getProgress() < 1.0 && img.isBackgroundLoading()) {
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0) {
                    javafx.application.Platform.runLater(() -> setCoverViewport(view, img));
                }
            });
        } else {
            setCoverViewport(view, img);
        }
        view.setImage(img);
    }

    private void setCoverViewport(ImageView view, Image img) {
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
        view.setFitWidth(160);
        view.setFitHeight(160);
        view.setPreserveRatio(false);
        Circle newClip = new Circle(80, 80, 80);
        view.setClip(newClip);
    }

    private void loadProfile() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            boolean isOwnProfile = currentUser.getId() == profileUser.getId();

            usernameLabel.setText("@" + profileUser.getUsername());
            nomeLabel.setText(profileUser.getName() != null ? profileUser.getName() : "");
            bioLabel.setText(profileUser.getBio() != null ? profileUser.getBio() : "");

            Image img = com.ale.tuney.ImageLoader.loadProfilePicture(profileUser.getProfileImageUrl());
            if (img != null) {
                applyCoverImage(profileImage, img);
            }

            ReviewDAO reviewDAO = new ReviewDAO();
            FollowDAO followDAO = new FollowDAO();

            String orderType = "Più recenti";
            if (reviewsSortOrder != null) {
                orderType = reviewsSortOrder.getValue();
            }
            List<Review> reviews = reviewDAO.findByUserIdSorted(profileUser.getId(), orderType);
            reviewsCount.setText(String.valueOf(reviews.size()));
            followersCount.setText(String.valueOf(followDAO.getFollowerCount(profileUser.getId())));
            followingCount.setText(String.valueOf(followDAO.getFollowingCount(profileUser.getId())));

            if (isOwnProfile) {
                followButton.setText("Modifica profilo");
            } else {
                boolean isFollowing = followDAO.isFollowing(currentUser.getId(), profileUser.getId());
                updateFollowButton(isFollowing);
            }

            hasFavorites = loadFavorites();
            caricaReviews(reviews);
            showReviewsSection();
            setActiveTab(feedTabUnderline);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateFollowButton(boolean isFollowing) {
        if (isFollowing) {
            followButton.setText("Smetti di seguire");
            followButton.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: white; -fx-background-radius: 18; -fx-pref-height: 36; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: white; -fx-border-radius: 18;");
        } else {
            followButton.setText("Segui");
            followButton.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 18; -fx-pref-height: 36; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: bold;");
        }
    }

    private void toggleFollow() {
        try {
            FollowDAO followDAO = new FollowDAO();
            int currentUserId = SessionManager.getCurrentUser().getId();
            boolean isFollowing = followDAO.isFollowing(currentUserId, profileUser.getId());

            ActivityDAO activityDAO = new ActivityDAO();
            if (isFollowing) {
                followDAO.unfollow(currentUserId, profileUser.getId());
                activityDAO.deleteByTypeAndUserAndReferenceId(currentUserId, "follow", profileUser.getId());
            } else {
                followDAO.follow(currentUserId, profileUser.getId());
                activityDAO.save(currentUserId, "follow", profileUser.getId());
            }

            updateFollowButton(!isFollowing);
            followersCount.setText(String.valueOf(followDAO.getFollowerCount(profileUser.getId())));

            // aggiorna subito la sezione Attività nella home
            if (followButton.getScene() != null) {
                BorderPane mainRoot = (BorderPane) followButton.getScene().getRoot();
                MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                if (mainCtrl != null) {
                    mainCtrl.loadActivity();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onFollowOrEditClick() {
        boolean isOwnProfile = SessionManager.getCurrentUser().getId() == profileUser.getId();
        if (isOwnProfile) {
            openEditProfile();
        } else {
            toggleFollow();
        }
    }

    private void openEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ale/tuney/edit-profile-view.fxml"));
            BorderPane editView = loader.load();
            EditProfileController controller = loader.getController();
            controller.setUser(profileUser);

            BorderPane currentRoot = (BorderPane) followButton.getScene().getRoot();
            currentRoot.setCenter(editView.getCenter());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void caricaReviews(List<Review> reviews) {
        updateReviewsStats(reviews);
        reviewsPane.getChildren().clear();
        for (Review review : reviews) {
            VBox card = creaCardReview(review);
            reviewsPane.getChildren().add(card);
        }

        boolean isOwnProfile = SessionManager.getCurrentUser() != null &&
                SessionManager.getCurrentUser().getId() == profileUser.getId();

        if (reviews.isEmpty()) {
            reviewsPane.setVisible(false);
            reviewsPane.setManaged(false);
            emptyReviewsLabel.setText(isOwnProfile
                    ? "Non hai ancora scritto recensioni."
                    : "L'utente non ha ancora scritto recensioni.");
            emptyReviewsBox.setVisible(true);
            emptyReviewsBox.setManaged(true);
        } else {
            reviewsPane.setVisible(true);
            reviewsPane.setManaged(true);
            emptyReviewsBox.setVisible(false);
            emptyReviewsBox.setManaged(false);
            Platform.runLater(() -> adjustGridWidth());
        }
    }

    private VBox creaCardReview(Review review) {
        VBox card = new VBox(8);
        card.setPrefWidth(155);
        card.setStyle("-fx-background-color: #090909; -fx-background-radius: 12; -fx-cursor: hand;");
        card.setPadding(new Insets(10));

        ImageView cover = new ImageView();
        cover.setFitWidth(135);
        cover.setFitHeight(135);
        cover.setPreserveRatio(false);

        String imageUrl = review.getTarget().getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            javafx.scene.image.Image img = com.ale.tuney.ImageLoader.load(imageUrl);
            if (img != null)
                cover.setImage(img);
        }

        String stars = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
        Label starsLabel = new Label(stars);
        starsLabel.setTextFill(Color.web("#ffe11f"));
        starsLabel.setFont(new Font(14));

        Label title = new Label(review.getTarget().getTitle());
        title.setTextFill(Color.WHITE);
        title.setFont(new Font(12));
        title.setWrapText(true);
        title.setPrefHeight(34);
        title.setMaxHeight(34);
        title.setAlignment(Pos.TOP_LEFT);
        title.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        card.getChildren().addAll(cover, starsLabel, title);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #090909; -fx-background-radius: 12; -fx-cursor: hand;"));

        card.setOnMouseClicked(e -> openReviewDetail(review, card));

        return card;
    }

    private void openReviewDetail(Review review, javafx.scene.Node source) {
        try {
            BorderPane mainRoot = (BorderPane) source.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewDetailController controller = loader.getController();
            controller.setReview(review);
            controller.setPreviousCenterNode(mainRoot.getCenter());
            mainRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void mostraFeed() {
        try {
            String orderType = "Più recenti";
            if (reviewsSortOrder != null) {
                orderType = reviewsSortOrder.getValue();
            }
            List<Review> reviews = new ReviewDAO().findByUserIdSorted(profileUser.getId(), orderType);
            caricaReviews(reviews);
            showReviewsSection();
            setActiveTab(feedTabUnderline);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showActivity() {
        try {
            ActivityDAO activityDAO = new ActivityDAO();
            List<Activity> activities = activityDAO.findByUser(profileUser.getId());
            showActivitySection(activities);
            if (activitySection != null) {
                activitySection.setMaxWidth(Double.MAX_VALUE);
            }
            setActiveTab(activityTabUnderline);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showReviewsSection() {
        profileContent.getChildren().clear();
        if (hasFavorites) {
            profileContent.getChildren().add(favoritesSection);
        }
        profileContent.getChildren().add(reviewsSection);
    }

    private boolean loadFavorites() {
        favoritesPane.getChildren().clear();
        boolean isOwnProfile = SessionManager.getCurrentUser() != null &&
                SessionManager.getCurrentUser().getId() == profileUser.getId();
        try {
            List<Item> favorites = new UserFavoritesDAO().findFavoritesByUserId(profileUser.getId());
            int count = 0;
            for (Item item : favorites) {
                if (item != null) {
                    VBox card = createFavCard(item);
                    favoritesPane.getChildren().add(card);
                    count++;
                }
            }

            if (count > 0) {
                favoritesPane.setVisible(true);
                favoritesPane.setManaged(true);
                emptyFavoritesBox.setVisible(false);
                emptyFavoritesBox.setManaged(false);
            } else {
                favoritesPane.setVisible(false);
                favoritesPane.setManaged(false);
                if (isOwnProfile) {
                    emptyFavoritesLabel.setText("Non hai ancora aggiunto brani o album preferiti.");
                    addFavoritesBtn.setVisible(true);
                    addFavoritesBtn.setManaged(true);
                } else {
                    emptyFavoritesLabel.setText("L'utente non ha ancora aggiunto elementi preferiti.");
                    addFavoritesBtn.setVisible(false);
                    addFavoritesBtn.setManaged(false);
                }
                emptyFavoritesBox.setVisible(true);
                emptyFavoritesBox.setManaged(true);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private VBox createFavCard(Item item) {
        VBox card = new VBox(8);
        card.setPrefWidth(155);
        card.setStyle("-fx-background-color: #090909; -fx-background-radius: 12; -fx-cursor: hand;");
        card.setPadding(new Insets(10));

        ImageView cover = new ImageView();
        cover.setFitWidth(135);
        cover.setFitHeight(135);
        cover.setPreserveRatio(false);

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Image img = com.ale.tuney.ImageLoader.load(imageUrl);
            if (img != null)
                cover.setImage(img);
        }

        Label typeLabel = new Label(item instanceof Song ? "Brano" : "Album");
        typeLabel.setTextFill(Color.web("#9b6fd4"));
        typeLabel.setFont(new Font(12));

        Label title = new Label(item.getTitle());
        title.setTextFill(Color.WHITE);
        title.setFont(new Font(12));
        title.setWrapText(true);
        title.setPrefHeight(34);
        title.setMaxHeight(34);
        title.setAlignment(Pos.TOP_LEFT);
        title.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        Label artist = new Label(String.join(", ", item.getArtists()));
        artist.setTextFill(Color.LIGHTGRAY);
        artist.setFont(new Font(12));
        artist.setWrapText(true);
        artist.setPrefHeight(17);
        artist.setMaxHeight(17);
        artist.setAlignment(Pos.TOP_LEFT);
        artist.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        card.getChildren().addAll(cover, typeLabel, title, artist);

        card.setOnMouseClicked(e -> {
            try {
                BorderPane mainRoot = (BorderPane) card.getScene().getRoot();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ale/tuney/item-detail-view.fxml"));
                BorderPane loadedRoot = loader.load();
                ItemDetailController controller = loader.getController();
                controller.setItem(item);
                controller.setPreviousCenterNode(mainRoot.getCenter());
                mainRoot.setCenter(loadedRoot.getCenter());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        card.setOnMouseEntered(
                e -> card.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseExited(
                e -> card.setStyle("-fx-background-color: #090909; -fx-background-radius: 12; -fx-cursor: hand;"));

        return card;
    }

    private void showActivitySection(List<Activity> activities) {
        if (activitySection == null) {
            activitySection = new VBox(12);
        } else {
            activitySection.getChildren().clear();
        }

        Label title = new Label("Attività recenti");
        title.setTextFill(Color.WHITE);
        title.setFont(new Font(20));
        title.setStyle("-fx-font-weight: bold;");
        activitySection.getChildren().add(title);

        if (activities.isEmpty()) {
            Label msg = new Label("Nessuna attività recente.");
            msg.setTextFill(Color.GRAY);
            msg.setFont(new Font(16));
            activitySection.getChildren().add(msg);
        } else {
            VBox cardContainer = new VBox(12);
            cardContainer.setStyle("-fx-padding: 0;");

            for (Activity activity : activities) {
                VBox card = createActivityCard(activity);
                cardContainer.getChildren().add(card);
            }

            activitySection.getChildren().add(cardContainer);
        }

        profileContent.getChildren().clear();
        profileContent.getChildren().add(activitySection);
    }

    private void caricaReviewsOrdinate(String orderType) {
        try {
            ReviewDAO reviewDAO = new ReviewDAO();
            List<Review> reviews = reviewDAO.findByUserIdSorted(profileUser.getId(), orderType);
            caricaReviews(reviews);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateReviewsStats(List<Review> reviews) {
        if (reviewsStatsBox == null || statsDataBox == null || emptyStatsBox == null) {
            return;
        }

        boolean hasReviews = reviews != null && !reviews.isEmpty();
        boolean isOwnProfile = SessionManager.getCurrentUser() != null &&
                profileUser != null &&
                SessionManager.getCurrentUser().getId() == profileUser.getId();

        if (!hasReviews) {
            statsDataBox.setVisible(false);
            statsDataBox.setManaged(false);
            if (emptyStatsLabel != null) {
                emptyStatsLabel.setText(isOwnProfile
                        ? "Non è possibile calcolare le statistiche: recensisci qualcosa per iniziare!"
                        : "Non è possibile calcolare le statistiche: l'utente non ha ancora scritto recensioni.");
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
            totalReviewsSublabel.setText(totalReviews == 1 ? "media su 1 recensione"
                    : String.format("media su %d recensioni", totalReviews));
        }

        ProgressBar[] progressBars = { bar1, bar2, bar3, bar4, bar5 };
        Label[] countLabels = { count1, count2, count3, count4, count5 };

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

    private VBox createActivityCard(Activity activity) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #090909; -fx-background-radius: 12;");
        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);

        Circle avatarBg = new Circle(16, Color.web("#3a3a3a"));
        ImageView avatar = new ImageView();
        avatar.setFitWidth(32);
        avatar.setFitHeight(32);
        avatar.setPreserveRatio(false);
        Circle clip = new Circle(16, 16, 16);
        avatar.setClip(clip);

        Image img = com.ale.tuney.ImageLoader.loadProfilePicture(activity.getUser().getProfileImageUrl());
        if (img != null)
            avatar.setImage(img);

        StackPane avatarStack = new StackPane(avatarBg, avatar);
        avatarStack.setPrefSize(32, 32);
        avatarStack.setMinSize(32, 32);
        avatarStack.setMaxSize(32, 32);

        TextFlow flow = new TextFlow();

        if ("review".equals(activity.getType())) {
            Text actionText = new Text("Ha recensito ");
            actionText.setFill(Color.WHITE);
            actionText.setStyle("-fx-font-size: 15;");
            flow.getChildren().add(actionText);

            Review review = activity.getReview();
            String title = review != null && review.getTarget() != null ? review.getTarget().getTitle()
                    : "Brano sconosciuto";
            Text itemText = new Text("\"" + title + "\"");
            itemText.setFill(Color.web("#9b6fd4"));
            itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-cursor: hand;");

            if (review != null) {
                int rating = review.getRating();
                itemText.setOnMouseClicked(e -> {
                    if (card.getScene() != null) {
                        BorderPane mainRoot = (BorderPane) card.getScene().getRoot();
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
                            BorderPane loadedRoot = loader.load();
                            ReviewDetailController controller = loader.getController();
                            controller.setReview(review);
                            controller.setPreviousCenterNode(mainRoot.getCenter());
                            mainRoot.setCenter(loadedRoot.getCenter());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                flow.getChildren().add(itemText);

                String stars = " " + "★".repeat(rating) + "☆".repeat(5 - rating);
                Text starsText = new Text(stars);
                starsText.setFill(Color.WHITE);
                starsText.setStyle("-fx-font-size: 15;");
                flow.getChildren().add(starsText);
            } else {
                flow.getChildren().add(itemText);
            }

        } else if ("follow".equals(activity.getType())) {
            Text actionText = new Text("Ha seguito un nuovo utente");
            actionText.setFill(Color.WHITE);
            actionText.setStyle("-fx-font-size: 15;");
            flow.getChildren().add(actionText);

        } else if ("like".equals(activity.getType())) {
            Text actionText = new Text("Ha messo \"Mi piace\" alla recensione di ");
            actionText.setFill(Color.WHITE);
            actionText.setStyle("-fx-font-size: 15;");
            flow.getChildren().add(actionText);

            Review review = activity.getReview();
            if (review != null) {
                Text authorText = new Text("@" + review.getAuthor().getUsername());
                authorText.setFill(Color.web("#ffb3d9"));
                authorText.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-cursor: hand;");
                authorText.setOnMouseClicked(e -> {
                    if (card.getScene() != null) {
                        BorderPane mainRoot = (BorderPane) card.getScene().getRoot();
                        MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                        if (mainCtrl != null) {
                            mainCtrl.openUserProfile(review.getAuthor());
                        }
                    }
                });
                flow.getChildren().add(authorText);

                Text suText = new Text(" su ");
                suText.setFill(Color.WHITE);
                suText.setStyle("-fx-font-size: 15;");
                flow.getChildren().add(suText);

                String title = review.getTarget() != null ? review.getTarget().getTitle() : "Brano sconosciuto";
                Text itemText = new Text("\"" + title + "\"");
                itemText.setFill(Color.web("#9b6fd4"));
                itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-cursor: hand;");
                itemText.setOnMouseClicked(e -> {
                    if (card.getScene() != null) {
                        BorderPane mainRoot = (BorderPane) card.getScene().getRoot();
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
                            BorderPane loadedRoot = loader.load();
                            ReviewDetailController controller = loader.getController();
                            controller.setReview(review);
                            controller.setPreviousCenterNode(mainRoot.getCenter());
                            mainRoot.setCenter(loadedRoot.getCenter());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                flow.getChildren().add(itemText);
            } else {
                Text genericText = new Text("una recensione");
                genericText.setFill(Color.WHITE);
                genericText.setStyle("-fx-font-size: 15;");
                flow.getChildren().add(genericText);
            }

        } else if ("comment".equals(activity.getType())) {
            Text actionText = new Text("Ha commentato la recensione di ");
            actionText.setFill(Color.WHITE);
            actionText.setStyle("-fx-font-size: 15;");
            flow.getChildren().add(actionText);

            Review review = activity.getReview();
            if (review != null) {
                Text authorText = new Text("@" + review.getAuthor().getUsername());
                authorText.setFill(Color.web("#ffb3d9"));
                authorText.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-cursor: hand;");
                authorText.setOnMouseClicked(e -> {
                    if (card.getScene() != null) {
                        BorderPane mainRoot = (BorderPane) card.getScene().getRoot();
                        MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                        if (mainCtrl != null) {
                            mainCtrl.openUserProfile(review.getAuthor());
                        }
                    }
                });
                flow.getChildren().add(authorText);

                Text suText = new Text(" su ");
                suText.setFill(Color.WHITE);
                suText.setStyle("-fx-font-size: 15;");
                flow.getChildren().add(suText);

                String title = review.getTarget() != null ? review.getTarget().getTitle() : "Brano sconosciuto";
                Text itemText = new Text("\"" + title + "\"");
                itemText.setFill(Color.web("#9b6fd4"));
                itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-cursor: hand;");
                itemText.setOnMouseClicked(e -> {
                    if (card.getScene() != null) {
                        BorderPane mainRoot = (BorderPane) card.getScene().getRoot();
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
                            BorderPane loadedRoot = loader.load();
                            ReviewDetailController controller = loader.getController();
                            controller.setReview(review);
                            controller.setPreviousCenterNode(mainRoot.getCenter());
                            mainRoot.setCenter(loadedRoot.getCenter());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                flow.getChildren().add(itemText);
            } else {
                Text genericText = new Text("una recensione");
                genericText.setFill(Color.WHITE);
                genericText.setStyle("-fx-font-size: 15;");
                flow.getChildren().add(genericText);
            }

        } else if ("favorite".equals(activity.getType())) {
            Text actionText = new Text("Ha aggiunto ");
            actionText.setFill(Color.WHITE);
            actionText.setStyle("-fx-font-size: 15;");
            flow.getChildren().add(actionText);

            com.ale.tuney.model.Item favItem = activity.getItem();
            String favTitle = favItem != null ? favItem.getTitle() : "un brano";
            Text itemText = new Text("\"" + favTitle + "\"");
            itemText.setFill(Color.web("#9b6fd4"));
            itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 15;");
            flow.getChildren().add(itemText);

            Text suffixText = new Text(" ai suoi Preferiti");
            suffixText.setFill(Color.WHITE);
            suffixText.setStyle("-fx-font-size: 15;");
            flow.getChildren().add(suffixText);

        } else {
            Text actionText = new Text("Ha fatto qualcosa");
            actionText.setFill(Color.WHITE);
            actionText.setStyle("-fx-font-size: 15;");
            flow.getChildren().add(actionText);
        }

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setFillHeight(false);

        Label timeLabel = new Label(timeAgo(activity.getCreatedAt()));
        timeLabel.setTextFill(Color.GRAY);
        timeLabel.setFont(new Font(12));
        timeLabel.setMinWidth(Region.USE_PREF_SIZE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content.getChildren().addAll(avatarStack, flow, spacer, timeLabel);

        card.getChildren().clear();
        card.getChildren().add(content);
        return card;
    }

    private void setActiveTab(Rectangle tabUnderline) {
        feedTabUnderline.setFill(Color.TRANSPARENT);
        activityTabUnderline.setFill(Color.TRANSPARENT);
        colorSVG(feedTab, Color.WHITE);
        colorSVG(attivitaTab, Color.WHITE);

        if (tabUnderline != null) {
            tabUnderline.setFill(Color.web("#9b6fd4"));
        }
        if (tabUnderline == feedTabUnderline) {
            colorSVG(feedTab, Color.web("#9b6fd4"));
        } else if (tabUnderline == activityTabUnderline) {
            colorSVG(attivitaTab, Color.web("#9b6fd4"));
        }
    }

    private void colorSVG(javafx.scene.Node node, Color colore) {
        if (node instanceof javafx.scene.shape.SVGPath svg) {
            svg.setFill(colore);
        } else if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                colorSVG(child, colore);
            }
        }
    }

    private String timeAgo(String createdAt) {
        try {
            java.time.LocalDateTime data = java.time.LocalDateTime.parse(
                    createdAt.replace(" ", "T"));
            // conversione timestamp
            java.time.ZonedDateTime dataUtc = data.atZone(java.time.ZoneOffset.UTC);
            java.time.ZonedDateTime dataLocale = dataUtc.withZoneSameInstant(java.time.ZoneId.systemDefault());

            java.time.Duration diff = java.time.Duration.between(dataLocale, java.time.ZonedDateTime.now());

            long minutes = diff.toMinutes();
            long hours = diff.toHours();
            long days = diff.toDays();

            if (minutes < 1)
                return "ora";
            if (minutes < 60)
                return minutes + " min fa";
            if (hours < 24)
                return hours + " h fa";
            if (days < 7)
                return days + " g fa";
            return dataLocale.toLocalDate().toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void updateFollowCount() {
        try {
            FollowDAO followDAO = new FollowDAO();
            followersCount.setText(String.valueOf(followDAO.getFollowerCount(profileUser.getId())));
            followingCount.setText(String.valueOf(followDAO.getFollowingCount(profileUser.getId())));

            User currentUser = SessionManager.getCurrentUser();
            if (currentUser != null && currentUser.getId() != profileUser.getId()) {
                boolean isFollowing = followDAO.isFollowing(currentUser.getId(), profileUser.getId());
                updateFollowButton(isFollowing);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openFollowersPopup() {
        try {
            List<User> users = new FollowDAO().getFollowers(profileUser.getId());
            showUsersPopup("Follower", users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openFollowingPopup() {
        try {
            List<User> users = new FollowDAO().getFollowing(profileUser.getId());
            showUsersPopup("Seguiti", users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showUsersPopup(String title, List<User> users) {
        Stage ownerStage = (Stage) followButton.getScene().getWindow();
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner(ownerStage);
        popup.setTitle(title);

        Label headerLabel = new Label(title);
        headerLabel.setTextFill(Color.WHITE);
        headerLabel.setFont(new Font("System Bold", 16));
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(headerLabel, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa;"
                + " -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        closeBtn.setOnAction(e -> popup.close());

        HBox header = new HBox(headerLabel, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 12, 20));
        header.setStyle("-fx-border-color: transparent transparent #2a2a2a transparent; -fx-border-width: 0 0 1 0;");

        VBox lista = new VBox(0);
        lista.setPadding(new Insets(8, 0, 8, 0));

        if (users.isEmpty()) {
            Label empty = new Label("Nessun utente da mostrare");
            empty.setTextFill(Color.GRAY);
            empty.setFont(new Font(13));
            empty.setPadding(new Insets(16, 20, 16, 20));
            lista.getChildren().add(empty);
        } else {
            for (User u : users) {
                HBox row = createUserRow(u, popup);
                lista.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");
        scroll.setMaxHeight(360);

        VBox root = new VBox(header, scroll);
        root.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 16;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 24, 0, 0, 8);");
        root.setPrefWidth(340);
        root.setMaxWidth(340);

        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPadding(new Insets(40));
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay)
                popup.close();
        });

        javafx.scene.Scene scene = new javafx.scene.Scene(overlay, 600, 500);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/com/ale/tuney/style.css").toExternalForm());

        popup.setScene(scene);
        popup.setX(ownerStage.getX() + (ownerStage.getWidth() - scene.getWidth()) / 2);
        popup.setY(ownerStage.getY() + (ownerStage.getHeight() - scene.getHeight()) / 2);
        popup.show();
    }

    private HBox createUserRow(User u, Stage popup) {
        Circle avatarBg = new Circle(20, Color.web("#3a3a3a"));
        ImageView avatar = new ImageView();
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(false);
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        Image img = com.ale.tuney.ImageLoader.loadProfilePicture(u.getProfileImageUrl());
        if (img != null)
            avatar.setImage(img);

        StackPane avatarStack = new StackPane(avatarBg, avatar);
        avatarStack.setPrefSize(40, 40);
        avatarStack.setMinSize(40, 40);
        avatarStack.setMaxSize(40, 40);

        Label usernameL = new Label("@" + u.getUsername());
        usernameL.setTextFill(Color.WHITE);
        usernameL.setFont(new Font("System Bold", 13));

        Label nomeL = new Label(u.getName() != null && !u.getName().isEmpty() ? u.getName() : "");
        nomeL.setTextFill(Color.GRAY);
        nomeL.setFont(new Font(12));

        VBox text = new VBox(2, usernameL);
        if (!nomeL.getText().isEmpty())
            text.getChildren().add(nomeL);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button actionBtn = null;
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && currentUser.getId() != u.getId()) {
            actionBtn = new Button();
            actionBtn.setMinWidth(Region.USE_PREF_SIZE);
            actionBtn.setMinHeight(Region.USE_PREF_SIZE);

            try {
                FollowDAO followDAO = new FollowDAO();
                boolean isFollowing = followDAO.isFollowing(currentUser.getId(), u.getId());

                if (isFollowing) {
                    actionBtn.setText("Smetti di seguire");
                    actionBtn.getStyleClass().add("dialog-btn-unfollow");
                } else {
                    actionBtn.setText("Segui");
                    actionBtn.getStyleClass().add("dialog-btn-follow");
                }

                final Button finalBtn = actionBtn;
                actionBtn.setOnAction(event -> {
                    event.consume();
                    try {
                        FollowDAO fDAO = new FollowDAO();
                        boolean currentlyFollowing = fDAO.isFollowing(currentUser.getId(), u.getId());
                        ActivityDAO activityDAO = new ActivityDAO();
                        if (currentlyFollowing) {
                            fDAO.unfollow(currentUser.getId(), u.getId());
                            activityDAO.deleteByTypeAndUserAndReferenceId(currentUser.getId(), "follow", u.getId());
                            finalBtn.setText("Segui");
                            finalBtn.getStyleClass().remove("dialog-btn-unfollow");
                            finalBtn.getStyleClass().add("dialog-btn-follow");
                        } else {
                            fDAO.follow(currentUser.getId(), u.getId());
                            activityDAO.save(currentUser.getId(), "follow", u.getId());
                            finalBtn.setText("Smetti di seguire");
                            finalBtn.getStyleClass().remove("dialog-btn-follow");
                            finalBtn.getStyleClass().add("dialog-btn-unfollow");
                        }

                        updateFollowCount();

                        if (followButton.getScene() != null) {
                            BorderPane mainRoot = (BorderPane) followButton.getScene().getRoot();
                            MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                            if (mainCtrl != null) {
                                mainCtrl.loadActivity();
                            }
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        HBox row;
        if (actionBtn != null) {
            row = new HBox(12, avatarStack, text, actionBtn);
        } else {
            row = new HBox(12, avatarStack, text);
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 20, 10, 20));
        row.setStyle("-fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #2a2a2a; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"));

        row.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) {
                return;
            }
            popup.close();
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/ale/tuney/profile-view.fxml"));
                BorderPane loadedRoot = loader.load();
                ProfileController controller = loader.getController();
                controller.setUser(u);
                BorderPane mainRoot = (BorderPane) followButton.getScene().getRoot();
                mainRoot.setCenter(loadedRoot.getCenter());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return row;
    }

    private void adjustGridWidth() {
        if (reviewsPane == null)
            return;

        double availableArea = profileScrollPane.getWidth() - 48;
        if (availableArea <= 0)
            return;

        reviewsPane.setPrefWrapLength(availableArea);
        reviewsPane.setPrefWidth(availableArea);
        reviewsPane.setMaxWidth(Double.MAX_VALUE);

        if (activitySection != null) {
            activitySection.setMaxWidth(Double.MAX_VALUE);
        }
    }
}
