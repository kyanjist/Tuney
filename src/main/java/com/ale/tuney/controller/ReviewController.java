package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.ThemeManager;
import javafx.application.Platform;
import com.ale.tuney.api.SpotifyClient;
import com.ale.tuney.dao.ActivityDAO;
import com.ale.tuney.dao.ReviewDAO;
import com.ale.tuney.model.Album;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.Review;
import com.ale.tuney.model.User;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.List;

public class ReviewController {

    @FXML
    private TextField searchField;
    @FXML
    private ListView<Item> resultsListView;
    @FXML
    private VBox reviewForm;
    @FXML
    private ImageView albumCover;
    @FXML
    private Label titleLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Label star1, star2, star3, star4, star5;
    @FXML
    private HBox starsBox;
    @FXML
    private TextArea reviewText;
    @FXML
    private Button submitButton;
    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();
    private Item currentItem;
    private int currentRating = 0;
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(600));
    // pausa per non chiamare spotify troppo spesso

    private Node previousCenterNode = null;

    public void setPreviousCenterNode(Node node) {
        this.previousCenterNode = node;
    }

    @FXML
    public void initialize() {

        // stelline cliccabili
        List<Label> stars = List.of(star1, star2, star3, star4, star5);
        for (int i = 0; i < stars.size(); i++) {
            final int rating = i + 1;
            Label s = stars.get(i);

            s.setOnMouseClicked(e -> {
                currentRating = rating;
                updateStars(stars, rating);
            });

            s.setOnMouseEntered(e -> updateStars(stars, rating));
            s.setOnMouseExited(e -> updateStars(stars, currentRating));
        }

        updateStars(stars, currentRating);

        // almeno due caratteri per la ricerca
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.stop();
            if (newVal == null || newVal.trim().length() < 2) {
                hideResults();
                return;
            }
            searchDebounce.setOnFinished(e -> search());
            searchDebounce.playFromStart();
        });

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        resultsListView.setCellFactory(lv -> new ListCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label titleLabel = new Label();
            private final Label artistLabel = new Label();
            private final Label typeLabel = new Label();
            private final VBox textBox = new VBox(4, titleLabel, artistLabel, typeLabel);
            private final HBox container = new HBox(10, imageView, textBox);

            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                imageView.setFitWidth(50);
                imageView.setFitHeight(50);
                imageView.setImage(com.ale.tuney.ImageLoader.load(item.getImageUrl()));

                titleLabel.setText(item.getTitle());
                titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                artistLabel.setText(String.join(", ", item.getArtists()));
                artistLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 12;");

                typeLabel.setText(item instanceof Album ? "Album" : "Brano");
                typeLabel.setStyle("-fx-text-fill: #9b6fd4; -fx-font-size: 11;");

                setText(null);
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(container);
            }
        });
        Platform.runLater(() -> {
            if (resultsListView.getScene() != null) {
                ThemeManager.applyTheme(resultsListView.getScene().getRoot());
            }
        });
    }

    private void updateStars(List<Label> stars, int rating) {
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setStyle(
                    "-fx-cursor: hand; -fx-text-fill: "
                            + (i < rating ? "#ffe11f" : "#444") + ";");
        }
    }

    @FXML
    public void search() {
        String query = searchField.getText();
        if (query == null || query.isBlank())
            return;

        Task<List<Object>> task = new Task<>() {
            @Override
            protected List<Object> call() {
                return SpotifyClient.search(query);
            }
        };

        task.setOnSucceeded(e -> {
            resultsListView.getItems().clear();
            for (Object obj : task.getValue()) {
                if (obj instanceof Item item)
                    resultsListView.getItems().add(item);
            }
            boolean has = !resultsListView.getItems().isEmpty();
            resultsListView.setVisible(has);
            resultsListView.setManaged(has);
        });

        task.setOnFailed(e -> hideResults());

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void selectResult() {
        Item sel = resultsListView.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;

        // se l'utente ha già recensito l'item apre la schermata di modifica
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            try {
                if (reviewDAO.existsByUserAndItem(currentUser.getId(), sel.getId())) {
                    hideResults();
                    Review existing = reviewDAO.findByUserAndItem(currentUser.getId(), sel.getId());
                    if (existing != null) {
                        openEditReview(existing);
                    }
                    return;
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }

        currentItem = sel;
        titleLabel.setText(sel.getTitle());
        artistLabel.setText(String.join(", ", sel.getArtists()));
        if (sel.getImageUrl() != null && !sel.getImageUrl().isBlank()) {
            albumCover.setImage(com.ale.tuney.ImageLoader.load(sel.getImageUrl()));
        }
        hideResults();
        clearError();
        reviewForm.setVisible(true);
        reviewForm.setManaged(true);
    }

    private void openEditReview(Review review) {
        try {
            BorderPane mainRoot = (BorderPane) submitButton.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewDetailController controller = loader.getController();
            controller.setReviewAndEdit(review);
            controller.setPreviousCenterNode(mainRoot.getCenter());
            mainRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void hideResults() {
        resultsListView.setVisible(false);
        resultsListView.setManaged(false);
    }

    @FXML
    private void onSubmitReview() {
        clearError();
        String err = validateInput();
        if (err != null) {
            showError(err);
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Sessione scaduta. Effettua di nuovo il login.");
            return;
        }
        submitAsync(buildReview(currentUser));
    }

    @FXML
    public void cancelReview() {
        if (previousCenterNode != null) {
            BorderPane mainRoot = (BorderPane) searchField.getScene().getRoot();
            mainRoot.setCenter(previousCenterNode);
            ItemDetailController idc = (ItemDetailController) previousCenterNode.getProperties()
                    .get("itemDetailController");
            if (idc != null) {
                idc.loadReviews();
            }
            return;
        }
        reviewForm.setVisible(false);
        reviewForm.setManaged(false);
        searchField.clear();
        currentItem = null;
        resetForm();
    }

    private String validateInput() {
        if (currentItem == null)
            return "Nessun brano o album selezionato.";
        if (currentRating < 1 || currentRating > 5)
            return "Seleziona un voto.";
        String t = reviewText.getText();
        if (t == null || t.trim().isEmpty())
            return "La recensione non può essere vuota.";
        if (t.length() > 2000)
            return "Massimo 2000 caratteri.";
        return null;
    }

    private Review buildReview(User author) {
        return new Review(
                0, currentItem, author,
                currentRating,
                reviewText.getText().trim(),
                LocalDateTime.now().toString());
    }

    private void submitAsync(Review review) {
        setFormEnabled(false);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int reviewId = reviewDAO.save(review);
                activityDAO.save(review.getAuthor().getId(), "review", reviewId);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            setFormEnabled(true);
            if (searchField.getScene() != null) {
                BorderPane mainRoot = (BorderPane) searchField.getScene().getRoot();
                MainController mainCtrl = (MainController) mainRoot.getProperties().get("mainController");
                if (mainCtrl != null) {
                    mainCtrl.loadActivity();
                }
            }
            showSuccess();
            cancelReview();
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            setFormEnabled(true);
            Throwable ex = task.getException();
            if (ex != null && ex.getMessage() != null
                    && ex.getMessage().contains("Duplicate entry")) {
                showError("Hai già recensito questo brano o album.");
            } else {
                showError("Errore database. Riprova più tardi.");
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showSuccess() {
        Stage ownerStage = (Stage) submitButton.getScene().getWindow();
        Stage popup = new Stage();
        popup.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        popup.initOwner(ownerStage);
        popup.setTitle("Recensione inviata");

        Label titleLbl = new Label("Recensione inviata");
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setFont(new Font("System Bold", 18));

        Label msgLbl = new Label("La tua recensione è stata pubblicata!");
        msgLbl.setTextFill(Color.web("#b3b3b3"));
        msgLbl.setFont(new Font(14));
        msgLbl.setWrapText(true);

        Button btnOk = new Button("Ok");
        btnOk.getStyleClass().add("dialog-btn-confirm");
        btnOk.setOnAction(e -> popup.close());

        HBox buttons = new HBox(btnOk);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(10, titleLbl, msgLbl, buttons);
        card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 24, 0, 0, 8); -fx-padding: 20;");
        card.setPrefWidth(350);
        card.setMaxWidth(350);

        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane(card);
        overlay.setPadding(new Insets(30));
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay)
                popup.close();
        });

        ThemeManager.applyTheme(overlay);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/com/ale/tuney/style.css").toExternalForm());

        popup.setScene(scene);

        popup.setOnShown(e -> {
            popup.setX(ownerStage.getX() + (ownerStage.getWidth() - popup.getWidth()) / 2);
            popup.setY(ownerStage.getY() + (ownerStage.getHeight() - popup.getHeight()) / 2);
        });

        popup.show();
        popup.requestFocus();
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

    private void setFormEnabled(boolean enabled) {
        submitButton.setDisable(!enabled);
        reviewText.setDisable(!enabled);
        starsBox.setDisable(!enabled);
    }

    private void resetForm() {
        reviewText.clear();
        currentRating = 0;
        clearError();
    }

    public void setItem(Item item) {
        this.currentItem = item;
        if (item != null) {
            titleLabel.setText(item.getTitle());
            artistLabel.setText(String.join(", ", item.getArtists()));
            if (item.getImageUrl() != null)
                albumCover.setImage(com.ale.tuney.ImageLoader.load(item.getImageUrl()));
            reviewForm.setVisible(true);
            reviewForm.setManaged(true);
        }
    }
}
