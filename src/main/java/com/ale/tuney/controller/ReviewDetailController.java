package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.ThemeManager;
import com.ale.tuney.dao.ActivityDAO;
import com.ale.tuney.dao.ReviewDAO;
import com.ale.tuney.dao.LikeDAO;
import com.ale.tuney.dao.CommentDAO;
import com.ale.tuney.model.Review;
import com.ale.tuney.model.User;
import com.ale.tuney.model.Comment;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jspecify.annotations.NonNull;

import java.sql.SQLException;
import java.util.List;

public class ReviewDetailController {

    @FXML
    private ImageView albumCover;
    @FXML
    private Label titleLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Label authorLabel;
    @FXML
    private Label star1, star2, star3, star4, star5;
    @FXML
    private TextArea reviewText;
    @FXML
    private HBox ownerButtons;
    @FXML
    private HBox editButtons;
    @FXML
    private Button editButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button cancelEditButton;
    @FXML
    private SVGPath saveIcon;
    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private SVGPath likeIcon;
    @FXML
    private Label likeCountLabel;
    @FXML
    private Label commentCountLabel;
    @FXML
    private VBox commentsContainer;
    @FXML
    private TextField newCommentField;
    @FXML
    private Label saveCountLabel;

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();
    private final LikeDAO likeDAO = new LikeDAO();
    private final CommentDAO commentDAO = new CommentDAO();
    private Review currentReview;
    private int currentRating = 0;
    private boolean editMode = false;
    private Node previousCenterNode = null;
    private boolean isLiked = false;
    private int likeCount = 0;

    public void setPreviousCenterNode(Node node) {
        this.previousCenterNode = node;
    }

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        Platform.runLater(() -> {
            if (albumCover.getScene() != null) {
                ThemeManager.applyTheme(albumCover.getScene().getRoot());
            }
        });
    }

    public void setReview(Review review) {
        this.currentReview = review;
        this.currentRating = review.getRating();

        if (review.getTarget() != null) {
            titleLabel.setText(review.getTarget().getTitle());
            artistLabel.setText(String.join(", ", review.getTarget().getArtists()));
            if (review.getTarget().getImageUrl() != null && !review.getTarget().getImageUrl().isBlank()) {
                albumCover.setImage(com.ale.tuney.ImageLoader.load(review.getTarget().getImageUrl()));
            }
            updateSaveStatus();
            updateSaveCount();
        }

        authorLabel.setText("@" + review.getAuthor().getUsername());
        authorLabel.setStyle("-fx-text-fill: #ffb3d9; -fx-cursor: hand;");
        authorLabel.setOnMouseClicked(e -> {
            openUserProfile(review.getAuthor(), authorLabel);
        });
        reviewText.setText(review.getText());

        List<Label> stars = List.of(star1, star2, star3, star4, star5);
        updateStars(stars, currentRating);

        User currentUser = SessionManager.getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getId() == review.getAuthor().getId();

        if (isOwner) {
            for (int i = 0; i < stars.size(); i++) {
                final int rating = i + 1;
                Label s = stars.get(i);
                s.setOnMouseClicked(e -> {
                    if (editMode) {
                        currentRating = rating;
                        updateStars(stars, rating);
                    }
                });
                s.setOnMouseEntered(e -> {
                    if (editMode)
                        updateStars(stars, rating);
                });
                s.setOnMouseExited(e -> {
                    if (editMode)
                        updateStars(stars, currentRating);
                });
            }
            editButton.setVisible(true);
            editButton.setManaged(true);
            deleteButton.setVisible(true);
            deleteButton.setManaged(true);
        } else {
            editButton.setVisible(false);
            editButton.setManaged(false);
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
        }

        saveButton.setVisible(false);
        saveButton.setManaged(false);
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        editButtons.setVisible(false);
        editButtons.setManaged(false);
        ownerButtons.setVisible(isOwner);
        ownerButtons.setManaged(isOwner);

        reviewText.setEditable(false);
        reviewText.setStyle("-fx-control-inner-background: #0d0d0d; -fx-text-fill: white; "
                + "-fx-border-color: transparent; -fx-background-radius: 8; -fx-font-size: 14px;");

        updateLikeStatus();
        updateLikeCount();
        loadComments();
    }

    public void setReviewAndEdit(Review review) {
        setReview(review);
        onEdit();
    }

    private void updateStars(List<Label> stars, int rating) {
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setStyle(
                    "-fx-cursor: " + (editMode ? "hand" : "default") + "; -fx-text-fill: "
                            + (i < rating ? "#ffe11f" : "#444") + ";");
        }
    }

    @FXML
    private void onEdit() {
        editMode = true;
        reviewText.setEditable(true);
        reviewText.setStyle("-fx-control-inner-background: #090909; -fx-text-fill: white; "
                + "-fx-border-color: #333; -fx-background-radius: 8; "
                + "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-font-size: 14px;");

        List<Label> stars = List.of(star1, star2, star3, star4, star5);
        updateStars(stars, currentRating);

        editButton.setVisible(false);
        editButton.setManaged(false);
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
        saveButton.setVisible(true);
        saveButton.setManaged(true);
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        ownerButtons.setVisible(false);
        ownerButtons.setManaged(false);
        editButtons.setVisible(true);
        editButtons.setManaged(true);
    }

    @FXML
    private void onCancelEdit() {
        editMode = false;
        reviewText.setText(currentReview.getText());
        reviewText.setEditable(false);
        reviewText.setStyle("-fx-control-inner-background: #0d0d0d; -fx-text-fill: white; "
                + "-fx-border-color: transparent; -fx-background-radius: 8; -fx-font-size: 14px;");
        currentRating = currentReview.getRating();
        List<Label> stars = List.of(star1, star2, star3, star4, star5);
        updateStars(stars, currentRating);

        saveButton.setVisible(false);
        saveButton.setManaged(false);
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        editButton.setVisible(true);
        editButton.setManaged(true);
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);
        editButtons.setVisible(false);
        editButtons.setManaged(false);
        ownerButtons.setVisible(true);
        ownerButtons.setManaged(true);
        clearError();
    }

    @FXML
    private void onSave() {
        clearError();
        String testo = reviewText.getText();
        if (currentRating < 1 || currentRating > 5) {
            showError("Seleziona un voto.");
            return;
        }
        if (testo == null || testo.trim().isEmpty()) {
            showError("La recensione non può essere vuota.");
            return;
        }
        if (testo.length() > 2000) {
            showError("Massimo 2000 caratteri.");
            return;
        }

        saveButton.setDisable(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        int reviewId = currentReview.getId();
        int userId = SessionManager.getCurrentUser().getId();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                reviewDAO.update(reviewId, currentRating, testo.trim());
                activityDAO.save(userId, "review", reviewId);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            saveButton.setDisable(false);

            currentReview.setRating(currentRating);
            currentReview.setText(testo.trim());

            onCancelEdit();
            if (editButton.getScene() != null) {
                BorderPane mainRoot = (BorderPane) editButton.getScene().getRoot();
                MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                if (mainCtrl != null) {
                    mainCtrl.loadActivity();
                }
            }
            showSuccessMsg("Recensione aggiornata!");
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            saveButton.setDisable(false);
            showError("Errore durante il salvataggio. Riprova.");
            e.getSource().getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Elimina recensione");
        confirm.setHeaderText(null);
        confirm.setContentText("Sei sicuro di voler eliminare questa recensione? L'operazione è irreversibile.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                deleteAsync();
            }
        });
    }

    private void deleteAsync() {
        deleteButton.setDisable(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        int reviewId = currentReview.getId();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                reviewDAO.delete(reviewId);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            backToHome();
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            deleteButton.setDisable(false);
            showError("Errore durante l'eliminazione. Riprova.");
            e.getSource().getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showSuccessMsg(String msg) {
        Stage ownerStage = (Stage) editButton.getScene().getWindow();
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner(ownerStage);
        popup.setTitle("Successo");

        Label titleLbl = new Label("Successo");
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setFont(new Font("System Bold", 18));

        Label msgLbl = new Label(msg);
        msgLbl.setTextFill(Color.web("#b3b3b3"));
        msgLbl.setFont(new Font(14));
        msgLbl.setWrapText(true);

        Button btnOk = new Button("Ok");
        btnOk.getStyleClass().add("dialog-btn-confirm");
        btnOk.setOnAction(e -> popup.close());

        HBox buttons = new HBox(btnOk);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(4, 0, 0, 0));
        buttons.setMaxHeight(Region.USE_PREF_SIZE);

        VBox card = new VBox(10, titleLbl, msgLbl, buttons);
        card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 40, 0, 0, 4); -fx-padding: 20 20 16 20;");
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPadding(new Insets(40));
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay)
                popup.close();
        });

        ThemeManager.applyTheme(overlay);

        Scene scene = new Scene(overlay, 480, 240);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/com/ale/tuney/style.css").toExternalForm());

        popup.setScene(scene);
        popup.setX(ownerStage.getX() + (ownerStage.getWidth() - scene.getWidth()) / 2);
        popup.setY(ownerStage.getY() + (ownerStage.getHeight() - scene.getHeight()) / 2);
        popup.showAndWait();
    }

    @FXML
    private void backToHome() {
        if (previousCenterNode != null) {
            BorderPane mainRoot = (BorderPane) editButton.getScene().getRoot();
            mainRoot.setCenter(previousCenterNode);

            MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
            if (mainCtrl != null) {
                mainCtrl.reloadActiveTab();
                mainCtrl.loadActivity();
            }

            ProfileController pc = (ProfileController) previousCenterNode.getProperties().get("profileController");
            if (pc != null) {
                pc.setUser(pc.getProfileUser());
            }

            ItemDetailController idc = (ItemDetailController) previousCenterNode.getProperties()
                    .get("itemDetailController");
            if (idc != null) {
                idc.loadReviews();
            }
        }
    }

    private void updateSaveStatus() {
        if (currentReview == null || currentReview.getTarget() == null)
            return;
        com.ale.tuney.model.User user = SessionManager.getCurrentUser();
        if (user == null)
            return;
        com.ale.tuney.dao.SavedItemDAO savedItemDAO = new com.ale.tuney.dao.SavedItemDAO();
        try {
            if (savedItemDAO.isSaved(user.getId(), currentReview.getTarget().getId())) {
                saveIcon.setContent("M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z");
                saveIcon.setFill(Color.web("#6b3fa0"));
                saveIcon.setStroke(Color.web("#6b3fa0"));
            } else {
                saveIcon.setContent("M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z");
                saveIcon.setFill(Color.TRANSPARENT);
                saveIcon.setStroke(Color.web("#b3b3b3"));
            }
            saveIcon.setStrokeWidth(1.5);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSaveCount() {
        if (currentReview == null || currentReview.getTarget() == null)
            return;
        try {
            int count = new com.ale.tuney.dao.SavedItemDAO().getSaveCount(currentReview.getTarget().getId());
            saveCountLabel.setText(String.valueOf(count));
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleSaveItem() {
        if (currentReview == null || currentReview.getTarget() == null)
            return;
        com.ale.tuney.model.User user = SessionManager.getCurrentUser();
        if (user == null)
            return;
        com.ale.tuney.dao.SavedItemDAO savedItemDAO = new com.ale.tuney.dao.SavedItemDAO();
        try {
            if (savedItemDAO.isSaved(user.getId(), currentReview.getTarget().getId())) {
                savedItemDAO.unsave(user.getId(), currentReview.getTarget().getId());
            } else {
                savedItemDAO.save(user.getId(), currentReview.getTarget());
            }
            updateSaveStatus();
            updateSaveCount();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
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

    private void updateLikeStatus() {
        if (currentReview == null)
            return;
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null)
            return;
        try {
            boolean liked = likeDAO.hasLiked(currentUser.getId(), currentReview.getId());
            this.isLiked = liked;
            if (liked) {
                likeIcon.setFill(Color.web("#ff4757"));
                likeIcon.setStroke(Color.web("#ff4757"));
            } else {
                likeIcon.setFill(Color.TRANSPARENT);
                likeIcon.setStroke(Color.web("#b3b3b3"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateLikeCount() {
        if (currentReview == null)
            return;
        try {
            int count = likeDAO.getLikeCount(currentReview.getId());
            this.likeCount = count;
            likeCountLabel.setText(String.valueOf(count));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadComments() {
        if (currentReview == null)
            return;
        try {
            List<Comment> comments = commentDAO.findByReviewId(currentReview.getId());
            commentCountLabel.setText(String.valueOf(comments.size()));
            commentsContainer.getChildren().clear();

            for (Comment c : comments) {
                HBox row = createCommentRow(c);
                commentsContainer.getChildren().add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createCommentRow(Comment c) {
        Circle avatarBg = new Circle(14, Color.web("#3a3a3a"));
        ImageView avatar = new ImageView();
        avatar.setFitWidth(28);
        avatar.setFitHeight(28);
        avatar.setPreserveRatio(false);
        Circle clip = new Circle(14, 14, 14);
        avatar.setClip(clip);

        User author = c.getAuthor();
        Image img = com.ale.tuney.ImageLoader.loadProfilePicture(author.getProfileImageUrl());
        if (img != null)
            avatar.setImage(img);

        StackPane avatarStack = new StackPane(avatarBg, avatar);
        avatarStack.setPrefSize(28, 28);
        avatarStack.setMinSize(28, 28);
        avatarStack.setMaxSize(28, 28);

        Label usernameL = new Label("@" + author.getUsername());
        usernameL.setTextFill(Color.web("#ffb3d9"));
        usernameL.setFont(new Font("System Bold", 14));
        usernameL.setStyle("-fx-cursor: hand;");
        usernameL.setOnMouseClicked(e -> {
            openUserProfile(author, usernameL);
        });

        Label timeL = new Label(timeAgo(c.getCreatedAt()));
        timeL.setTextFill(Color.GRAY);
        timeL.setFont(new Font(13));

        HBox header = new HBox(10, usernameL, timeL);
        header.setAlignment(Pos.CENTER_LEFT);

        Label testoL = new Label(c.getText());
        testoL.setTextFill(Color.WHITE);
        testoL.setFont(new Font(14));
        testoL.setWrapText(true);
        testoL.setMaxWidth(480);

        User currentUser = SessionManager.getCurrentUser();
        HBox deleteBox = new HBox();
        if (currentUser != null && (currentUser.getId() == author.getId()
                || currentUser.getId() == currentReview.getAuthor().getId())) {
            Button deleteButton = new Button("Elimina");
            deleteButton.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 0;");
            deleteButton.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Elimina commento");
                confirm.setHeaderText(null);
                confirm.setContentText("Sei sicuro di voler eliminare questo commento?");
                confirm.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        try {
                            commentDAO.delete(c.getId());
                            loadComments();
                            if (editButton.getScene() != null) {
                                BorderPane mainRoot = (BorderPane) editButton.getScene().getRoot();
                                MainController mainCtrl = (MainController) mainRoot.getProperties()
                                        .get("mainController");
                                if (mainCtrl != null) {
                                    mainCtrl.loadActivity();
                                }
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            });
            deleteBox.getChildren().add(deleteButton);
        }

        VBox content = new VBox(4, header, testoL, deleteBox);
        content.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox row = new HBox(12, avatarStack, content);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 10;");
        return row;
    }

    private String timeAgo(String createdAt) {
        if (createdAt == null)
            return "";
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

    @FXML
    private void handleLikeClicked() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || currentReview == null)
            return;

        isLiked = !isLiked;
        if (isLiked) {
            likeCount++;
            likeIcon.setFill(Color.web("#ff4757"));
            likeIcon.setStroke(Color.web("#ff4757"));
        } else {
            likeCount--;
            likeIcon.setFill(Color.TRANSPARENT);
            likeIcon.setStroke(Color.web("#b3b3b3"));
        }
        likeCountLabel.setText(String.valueOf(likeCount));

        final boolean targetLiked = isLiked;
        Task<Void> likeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (targetLiked) {
                    likeDAO.like(currentUser.getId(), currentReview.getId());
                    try {
                        activityDAO.save(currentUser.getId(), "like", currentReview.getId());
                    } catch (SQLException ea) {
                        ea.printStackTrace();
                    }
                } else {
                    likeDAO.unlike(currentUser.getId(), currentReview.getId());
                }
                return null;
            }

            @Override
            protected void succeeded() {
                if (likeCountLabel.getScene() != null) {
                    BorderPane mainRoot = (BorderPane) likeCountLabel.getScene().getRoot();
                    MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                    if (mainCtrl != null) {
                        mainCtrl.loadActivity();
                    }
                }
            }

            @Override
            protected void failed() {
                getException().printStackTrace();
                isLiked = !targetLiked;
                if (isLiked) {
                    likeCount++;
                    likeIcon.setFill(Color.web("#ff4757"));
                    likeIcon.setStroke(Color.web("#ff4757"));
                } else {
                    likeCount--;
                    likeIcon.setFill(Color.TRANSPARENT);
                    likeIcon.setStroke(Color.web("#b3b3b3"));
                }
                likeCountLabel.setText(String.valueOf(likeCount));
            }
        };

        Thread t = new Thread(likeTask);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleShowLikers(MouseEvent event) {
        event.consume();
        if (currentReview == null)
            return;
        try {
            List<User> likers = likeDAO.getLikers(currentReview.getId());
            showUsersPopup("Utenti a cui piace", likers);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFocusComment() {
        newCommentField.requestFocus();
    }

    @FXML
    private void handleSendComment() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || currentReview == null)
            return;

        String text = newCommentField.getText();
        if (text == null || text.trim().isEmpty())
            return;

        Comment comment = new Comment(0, currentUser, currentReview.getId(), text.trim(), null);
        try {
            int commentId = commentDAO.save(comment);
            newCommentField.clear();

            if (commentId > 0) {
                try {
                    activityDAO.save(currentUser.getId(), "comment", commentId);
                } catch (SQLException ea) {
                    ea.printStackTrace();
                }
            }
            loadComments();

            if (newCommentField.getScene() != null) {
                BorderPane mainRoot = (BorderPane) newCommentField.getScene().getRoot();
                MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                if (mainCtrl != null) {
                    mainCtrl.loadActivity();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showUsersPopup(String title, List<User> users) {
        Stage ownerStage = (Stage) likeCountLabel.getScene().getWindow();
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
            Label empty = new Label("Nessun utente");
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

        Scene scene = getScene(header, scroll, popup);
        scene.getStylesheets().add(getClass().getResource("/com/ale/tuney/style.css").toExternalForm());

        popup.setScene(scene);

        popup.setX(ownerStage.getX() + (ownerStage.getWidth() - scene.getWidth()) / 2);
        popup.setY(ownerStage.getY() + (ownerStage.getHeight() - scene.getHeight()) / 2);
        popup.show();
    }

    private static @NonNull Scene getScene(HBox header, ScrollPane scroll, Stage popup) {
        VBox root = new VBox(header, scroll);
        root.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 16;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 24, 0, 0, 8);");
        root.setPrefWidth(340);
        root.setMaxWidth(340);

        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay)
                popup.close();
        });

        Scene scene = new Scene(overlay, 600, 500);
        scene.setFill(Color.TRANSPARENT);
        return scene;
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

        HBox row = new HBox(12, avatarStack, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 20, 10, 20));
        row.setStyle("-fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #2a2a2a; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"));

        row.setOnMouseClicked(e -> {
            popup.close();
            openUserProfile(u, likeCountLabel);
        });

        return row;
    }
}
