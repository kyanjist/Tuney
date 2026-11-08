package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.ThemeManager;
import com.ale.tuney.dao.SavedItemDAO;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.Comparator;

public class SavedItemsController implements Initializable {

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane feedPane;
    @FXML
    private VBox emptyVBox;
    @FXML
    private ChoiceBox<String> sortChoice;

    private final SavedItemDAO savedItemDAO = new SavedItemDAO();
    private final List<Item> currentItems = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        emptyVBox.setVisible(false);
        emptyVBox.setManaged(false);

        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> adjustGridWidth());
        scrollPane.widthProperty().addListener((obs, oldVal, newVal) -> adjustGridWidth());

        scrollPane.layoutBoundsProperty().addListener((obs, old, bounds) -> {
            Rectangle clip = new Rectangle(bounds.getWidth(), bounds.getHeight());
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            scrollPane.setClip(clip);
        });

        Platform.runLater(() -> {
            if (scrollPane.getScene() != null) {
                ThemeManager.applyTheme(scrollPane.getScene().getRoot());
            }
        });

        sortChoice.getItems().addAll(
                "Più recenti", "Meno recenti", "Titolo A-Z", "Titolo Z-A", "Artista A-Z");
        sortChoice.setValue("Più recenti");
        sortChoice.setOnAction(e -> renderItems());

        loadSaved();
    }

    private void loadSaved() {
        User user = SessionManager.getCurrentUser();
        if (user == null)
            return;

        Task<List<Item>> task = new Task<>() {
            @Override
            protected List<Item> call() throws Exception {
                return savedItemDAO.getSavedItems(user.getId());
            }
        };

        task.setOnSucceeded(e -> {
            currentItems.clear();
            currentItems.addAll(task.getValue());
            renderItems();
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            showEmpty(true);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void renderItems() {
        feedPane.getChildren().clear();

        if (currentItems.isEmpty()) {
            showEmpty(true);
            return;
        }
        showEmpty(false);

        List<Item> sorted = new ArrayList<>(currentItems);
        String criterio = sortChoice.getValue();
        if (criterio == null) criterio = "Più recenti";

        switch (criterio) {
            case "Meno recenti" -> {
                // currentItems è dal più recente: invertiamo
                java.util.Collections.reverse(sorted);
            }
            case "Titolo A-Z" -> sorted.sort(
                    Comparator.comparing(i -> i.getTitle().toLowerCase()));
            case "Titolo Z-A" -> sorted.sort(
                    Comparator.comparing((Item i) -> i.getTitle().toLowerCase()).reversed());
            case "Artista A-Z" -> sorted.sort(
                    Comparator.comparing(i -> String.join(", ", i.getArtists()).toLowerCase()));
            default -> { /* Più recenti: ordine originale */ }
        }

        for (Item item : sorted) {
            feedPane.getChildren().add(createItemCard(item));
        }
        Platform.runLater(() -> adjustGridWidth());
    }

    private VBox createItemCard(Item item) {
        VBox card = new VBox(5);
        card.setPrefWidth(155);
        card.setStyle("-fx-background-color: #090909; -fx-background-radius: 12; -fx-cursor: hand;");
        card.setPadding(new Insets(8));

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(135, 135);

        ImageView cover = new ImageView();
        cover.setFitWidth(135);
        cover.setFitHeight(135);
        cover.setPreserveRatio(false);
        cover.setImage(com.ale.tuney.ImageLoader.load(item.getImageUrl()));

        Rectangle imageClip = new Rectangle(135, 135);
        imageClip.setArcWidth(8);
        imageClip.setArcHeight(8);
        cover.setClip(imageClip);

        Button saveButton = new Button();
        SVGPath svg = new SVGPath();
        svg.setContent("M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z");
        svg.setFill(Color.WHITE);
        saveButton.setGraphic(svg);
        saveButton.setStyle(
                "-fx-background-color: #6b3fa0; -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 0;");
        saveButton.setPrefSize(32, 32);
        saveButton.setMinSize(32, 32);
        saveButton.setMaxSize(32, 32);
        StackPane.setAlignment(saveButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(saveButton, new Insets(6));

        saveButton.setOnMouseClicked(e -> {
            e.consume();
            removeElement(item, card);
        });

        imageContainer.getChildren().addAll(cover, saveButton);

        Label typeLabel = new Label(item instanceof com.ale.tuney.model.Song ? "Brano" : "Album");
        typeLabel.setTextFill(Color.web("#9b6fd4"));
        typeLabel.setFont(new Font(12));

        Label title = new Label(item.getTitle());
        title.setTextFill(Color.WHITE);
        title.setFont(new Font(12));
        title.setWrapText(false);
        title.setMaxWidth(135);
        title.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        Label artist = new Label(String.join(", ", item.getArtists()));
        artist.setTextFill(Color.LIGHTGRAY);
        artist.setFont(new Font(12));
        artist.setWrapText(false);
        artist.setMaxWidth(135);
        artist.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        card.getChildren().addAll(imageContainer, typeLabel, title, artist);

        card.setOnMouseEntered(
                e -> card.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseExited(
                e -> card.setStyle("-fx-background-color: #090909; -fx-background-radius: 12; -fx-cursor: hand;"));

        card.setOnMouseClicked(e -> openItemDetail(item, card));

        return card;
    }

    private void removeElement(Item item, VBox card) {
        User user = SessionManager.getCurrentUser();
        if (user == null)
            return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                savedItemDAO.unsave(user.getId(), item.getId());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            currentItems.remove(item);
            feedPane.getChildren().remove(card);
            if (feedPane.getChildren().isEmpty()) {
                showEmpty(true);
            } else {
                adjustGridWidth();
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showEmpty(boolean empty) {
        emptyVBox.setVisible(empty);
        emptyVBox.setManaged(empty);
        scrollPane.setVisible(!empty);
        scrollPane.setManaged(!empty);
    }

    private void openItemDetail(Item item, Node source) {
        try {
            BorderPane mainRoot = (BorderPane) source.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ale/tuney/item-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ItemDetailController ctrl = loader.getController();
            ctrl.setItem(item);
            ctrl.setPreviousCenterNode(mainRoot.getCenter());
            mainRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adjustGridWidth() {
        if (feedPane.getChildren().isEmpty())
            return;

        scrollPane.applyCss();
        scrollPane.layout();

        double availableArea = scrollPane.getViewportBounds().getWidth() - 30 + 18;

        Region primaCard = (Region) feedPane.getChildren().get(0);
        primaCard.applyCss();
        primaCard.layout();

        double cardWidth = primaCard.getBoundsInParent().getWidth();
        if (cardWidth <= 0)
            cardWidth = primaCard.getPrefWidth();

        double hgap = feedPane.getHgap();

        int columns = (int) ((availableArea + hgap) / (cardWidth + hgap));
        if (columns <= 0)
            columns = 1;

        double exactGridWidth = (columns * cardWidth) + ((columns - 1) * hgap);
        feedPane.setPrefWidth(exactGridWidth);
        feedPane.setMaxWidth(exactGridWidth);
    }
}
