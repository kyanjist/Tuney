package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.ThemeManager;
import javafx.application.Platform;
import com.ale.tuney.api.SpotifyClient;
import com.ale.tuney.dao.UserDAO;
import com.ale.tuney.model.Album;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.User;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SearchController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ListView<Object> resultsList;
    @FXML private Label noResultLabel;
    @FXML private Button allChip;
    @FXML private Button tracksChip;
    @FXML private Button albumsChip;
    @FXML private Button usersChip;

    private final UserDAO userDAO = new UserDAO();
    private String currentFilter = "tutti";
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(600));
    // pausa per non chiamare spotify troppo spesso

    private static final String CHIP_ACTIVE   = "-fx-background-color: #6b3fa0; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12;";
    private static final String CHIP_INACTIVE = "-fx-background-color: #2a2a2a; -fx-text-fill: #aaa; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        noResultLabel.setVisible(false);
        noResultLabel.setManaged(false);

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

        resultsList.setCellFactory(lv -> new ListCell<>() {
            private final ImageView imgView = new ImageView();
            private final Label linea1 = new Label();
            private final Label linea2 = new Label();
            private final Label badgeLabel = new Label();
            private final VBox textBox = new VBox(3, linea1, linea2);
            private final Button reviewButton = new Button();
            private final Button saveButton = new Button();
            private final HBox spacer = new HBox();
            private final HBox container = new HBox(12, imgView, textBox, spacer, reviewButton, saveButton);

            {
                imgView.setFitWidth(52);
                imgView.setFitHeight(52);
                container.setAlignment(Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                badgeLabel.setFont(new Font(12));
                badgeLabel.setPadding(new Insets(2, 6, 2, 6));
                badgeLabel.setStyle("-fx-background-radius: 6;");
                linea1.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
                linea2.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 12;");

                SVGPath addIcon = new SVGPath();
                addIcon.setContent("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
                addIcon.setFill(Color.WHITE);
                reviewButton.setGraphic(addIcon);
                reviewButton.setPrefWidth(36);
                reviewButton.setPrefHeight(36);
                reviewButton.setMinWidth(36);
                reviewButton.setMinHeight(36);
                reviewButton.setMaxWidth(36);
                reviewButton.setMaxHeight(36);
                reviewButton.setStyle(
                        "-fx-background-color: #6b3fa0; -fx-background-radius: 50%; -fx-cursor: hand; -fx-padding: 0;");

                saveButton.setPrefWidth(36);
                saveButton.setPrefHeight(36);
                saveButton.setMinWidth(36);
                saveButton.setMinHeight(36);
                saveButton.setMaxWidth(36);
                saveButton.setMaxHeight(36);
                saveButton.setStyle(
                        "-fx-background-color: #2a2a2a; -fx-background-radius: 50%; -fx-cursor: hand; -fx-padding: 0;");
                container.prefWidthProperty().bind(lv.widthProperty().subtract(30));
                container.setMaxWidth(Control.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                if (item instanceof User user) {
                    Circle clip = new Circle(26, 26, 26);
                    imgView.setClip(clip);
                    Image img = com.ale.tuney.ImageLoader.loadProfilePicture(user.getProfileImageUrl());
                    com.ale.tuney.ImageLoader.applyProfilePicture(imgView, img, 52);
                    linea1.setText("@" + user.getUsername());
                    linea1.setStyle("-fx-text-fill: #ffb3d9; -fx-font-weight: bold; -fx-font-size: 14;");
                    linea2.setText(user.getName() != null ? user.getName() : "");
                    badgeLabel.setText("Utente");
                    badgeLabel.setStyle(
                            "-fx-background-color: #2a1a3a; -fx-text-fill: #9b6fd4; -fx-background-radius: 6;");
                    if (!textBox.getChildren().contains(badgeLabel))
                        textBox.getChildren().add(badgeLabel);

                    reviewButton.setVisible(false);
                    reviewButton.setManaged(false);
                    saveButton.setVisible(false);
                    saveButton.setManaged(false);
                    spacer.setVisible(false);
                    spacer.setManaged(false);

                } else if (item instanceof Item spotItem) {
                    imgView.setClip(null);
                    imgView.setViewport(null);
                    imgView.setFitWidth(52);
                    imgView.setFitHeight(52);
                    imgView.setPreserveRatio(true);
                    imgView.setImage(com.ale.tuney.ImageLoader.load(spotItem.getImageUrl()));
                    linea1.setText(spotItem.getTitle());
                    linea1.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
                    linea2.setText(String.join(", ", spotItem.getArtists()));
                    boolean isAlbum = spotItem instanceof Album;
                    badgeLabel.setText(isAlbum ? "Album" : "Brano");
                    badgeLabel.setStyle(isAlbum
                            ? "-fx-background-color: #1a1a3a; -fx-text-fill: #8888ff; -fx-background-radius: 6;"
                            : "-fx-background-color: #3a1a1a; -fx-text-fill: #ff8888; -fx-background-radius: 6;");
                    if (!textBox.getChildren().contains(badgeLabel))
                        textBox.getChildren().add(badgeLabel);

                    reviewButton.setVisible(true);
                    reviewButton.setManaged(true);
                    saveButton.setVisible(true);
                    saveButton.setManaged(true);
                    spacer.setVisible(true);
                    spacer.setManaged(true);

                    com.ale.tuney.model.User currentUser = SessionManager.getCurrentUser();
                    if (currentUser != null) {
                        com.ale.tuney.dao.SavedItemDAO savedItemDAO = new com.ale.tuney.dao.SavedItemDAO();
                        try {
                            SVGPath svg = new SVGPath();
                            svg.setFill(Color.WHITE);
                            if (savedItemDAO.isSaved(currentUser.getId(), spotItem.getId())) {
                                svg.setContent("M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z");
                                saveButton.setGraphic(svg);
                                saveButton.setText("");
                                saveButton.setStyle(
                                        "-fx-background-color: #6b3fa0; -fx-background-radius: 50%; -fx-cursor: hand; -fx-padding: 0;");
                            } else {
                                svg.setContent(
                                        "M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15-5-2.18-5 2.18V5h10v13z");
                                saveButton.setGraphic(svg);
                                saveButton.setText("");
                                saveButton.setStyle(
                                        "-fx-background-color: #2a2a2a; -fx-background-radius: 50%; -fx-cursor: hand; -fx-padding: 0;");
                            }
                        } catch (java.sql.SQLException ex) {
                            ex.printStackTrace();
                        }
                    }

                    reviewButton.setOnMouseClicked(e -> {
                        e.consume();
                        com.ale.tuney.model.User currUserRec = SessionManager.getCurrentUser();
                        if (currUserRec != null) {
                            try {
                                com.ale.tuney.dao.ReviewDAO reviewDAOCheck = new com.ale.tuney.dao.ReviewDAO();
                                if (reviewDAOCheck.existsByUserAndItem(currUserRec.getId(), spotItem.getId())) {
                                    com.ale.tuney.model.Review existing = reviewDAOCheck
                                            .findByUserAndItem(currUserRec.getId(), spotItem.getId());
                                    if (existing != null) {
                                        openEditReview(existing);
                                        return;
                                    }
                                }
                            } catch (java.sql.SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                        openCreateReview(spotItem);
                    });

                    saveButton.setOnMouseClicked(e -> {
                        e.consume();
                        com.ale.tuney.model.User currUser = SessionManager.getCurrentUser();
                        if (currUser != null) {
                            com.ale.tuney.dao.SavedItemDAO savedItemDAO = new com.ale.tuney.dao.SavedItemDAO();
                            try {
                                SVGPath newSvg = new SVGPath();
                                newSvg.setFill(Color.WHITE);
                                if (savedItemDAO.isSaved(currUser.getId(), spotItem.getId())) {
                                    savedItemDAO.unsave(currUser.getId(), spotItem.getId());
                                    newSvg.setContent(
                                            "M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15-5-2.18-5 2.18V5h10v13z");
                                    saveButton.setGraphic(newSvg);
                                    saveButton.setText("");
                                    saveButton.setStyle(
                                            "-fx-background-color: #2a2a2a; -fx-background-radius: 50%; -fx-cursor: hand; -fx-padding: 0;");
                                } else {
                                    savedItemDAO.save(currUser.getId(), spotItem);
                                    newSvg.setContent("M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z");
                                    saveButton.setGraphic(newSvg);
                                    saveButton.setText("");
                                    saveButton.setStyle(
                                            "-fx-background-color: #6b3fa0; -fx-background-radius: 50%; -fx-cursor: hand; -fx-padding: 0;");
                                }
                            } catch (java.sql.SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

                setText(null);
                setGraphic(container);
            }
        });

        Platform.runLater(() -> {
            if (resultsList.getScene() != null) {
                ThemeManager.applyTheme(resultsList.getScene().getRoot());
            }
        });
    }

    @FXML
    public void search() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            hideResults();
            return;
        }

        Task<List<Object>> task = new Task<>() {
            @Override
            protected List<Object> call() {
                List<Object> results = new ArrayList<>();

                if ("tutti".equals(currentFilter) || "utenti".equals(currentFilter)) {
                    try {
                        List<User> users = userDAO.findByUsernameOrName(query);
                        results.addAll(users);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                if (!"utenti".equals(currentFilter)) {
                    List<Object> spotifyResults = SpotifyClient.search(query);
                    for (Object obj : spotifyResults) {
                        if (obj instanceof Album
                                && ("tutti".equals(currentFilter) || "album".equals(currentFilter))) {
                            results.add(obj);
                        } else if (obj instanceof Item && !(obj instanceof Album)
                                && ("tutti".equals(currentFilter) || "brani".equals(currentFilter))) {
                            results.add(obj);
                        }
                    }
                }

                return results;
            }
        };

        task.setOnSucceeded(e -> {
            List<Object> results = task.getValue();
            resultsList.getItems().clear();

            if (results.isEmpty()) {
                noResultLabel.setVisible(true);
                noResultLabel.setManaged(true);
                resultsList.setVisible(false);
                resultsList.setManaged(false);
            } else {
                noResultLabel.setVisible(false);
                noResultLabel.setManaged(false);
                resultsList.getItems().addAll(results);
                resultsList.setVisible(true);
                resultsList.setManaged(true);
            }
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            noResultLabel.setText("Errore durante la ricerca.");
            noResultLabel.setVisible(true);
            noResultLabel.setManaged(true);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void selectResult() {
        Object sel = resultsList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if (sel instanceof User user) {
            openUserProfile(user);
        } else if (sel instanceof Item item) {
            openItemDetail(item);
        }
    }

    private void openUserProfile(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ale/tuney/profile-view.fxml"));
            BorderPane profileView = loader.load();
            ProfileController ctrl = loader.getController();
            ctrl.setUser(user);

            BorderPane currentRoot = (BorderPane) searchField.getScene().getRoot();
            currentRoot.setCenter(profileView.getCenter());
            searchField.clear();
            hideResults();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openItemDetail(Item item) {
        try {
            BorderPane currentRoot = (BorderPane) searchField.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/item-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ItemDetailController ctrl = loader.getController();
            ctrl.setItem(item);
            ctrl.setPreviousCenterNode(currentRoot.getCenter());
            currentRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void allFilter()  { setFilter("tutti");  }
    @FXML private void tracksFilter()  { setFilter("brani");  }
    @FXML private void albumsFilter()  { setFilter("album");  }
    @FXML private void usersFilter() { setFilter("utenti"); }

    private void setFilter(String filtro) {
        currentFilter = filtro;
        allChip.setStyle(filtro.equals("tutti")   ? CHIP_ACTIVE : CHIP_INACTIVE);
        tracksChip.setStyle(filtro.equals("brani")   ? CHIP_ACTIVE : CHIP_INACTIVE);
        albumsChip.setStyle(filtro.equals("album")   ? CHIP_ACTIVE : CHIP_INACTIVE);
        usersChip.setStyle(filtro.equals("utenti") ? CHIP_ACTIVE : CHIP_INACTIVE);
        updateChipGraphicColor(tracksChip,  filtro.equals("brani"));
        updateChipGraphicColor(albumsChip,  filtro.equals("album"));
        updateChipGraphicColor(usersChip, filtro.equals("utenti"));
        if (!searchField.getText().isBlank()) search();
    }

    private void updateChipGraphicColor(Button button, boolean active) {
        if (button.getGraphic() instanceof SVGPath svg) {
            svg.setFill(Color.web(active ? "#ffffff" : "#aaaaaa"));
        }
    }

    private void hideResults() {
        resultsList.setVisible(false);
        resultsList.setManaged(false);
        noResultLabel.setVisible(false);
        noResultLabel.setManaged(false);
    }

    public TextField getSearchField() { return searchField; }
    public void triggerSearch() { search(); }

    private void openCreateReview(Item item) {
        try {
            BorderPane currentRoot = (BorderPane) searchField.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/review-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewController ctrl = loader.getController();
            ctrl.setItem(item);
            ctrl.setPreviousCenterNode(currentRoot.getCenter());
            currentRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditReview(com.ale.tuney.model.Review review) {
        try {
            BorderPane currentRoot = (BorderPane) searchField.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ale/tuney/review-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewDetailController ctrl = loader.getController();
            ctrl.setReviewAndEdit(review);
            ctrl.setPreviousCenterNode(currentRoot.getCenter());
            currentRoot.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
