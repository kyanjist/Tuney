package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.dao.UserDAO;
import com.ale.tuney.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ResourceBundle;
import com.ale.tuney.model.Item;
import com.ale.tuney.model.Song;
import com.ale.tuney.dao.UserFavoritesDAO;
import com.ale.tuney.dao.ActivityDAO;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.List;
import java.util.ArrayList;

public class EditProfileController implements Initializable {

    @FXML private ImageView profileImagePreview;
    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextArea bioField;
    @FXML private Label errorLabel;
    @FXML private Label bioCounterLabel;

    @FXML private ImageView fav1Cover;
    @FXML private Label fav1Title;
    @FXML private Label fav1Artist;
    @FXML private Button fav1RemoveBtn;
    @FXML private HBox fav1SearchBox;
    @FXML private TextField fav1SearchField;
    @FXML private ListView<Item> fav1Results;

    @FXML private ImageView fav2Cover;
    @FXML private Label fav2Title;
    @FXML private Label fav2Artist;
    @FXML private Button fav2RemoveBtn;
    @FXML private HBox fav2SearchBox;
    @FXML private TextField fav2SearchField;
    @FXML private ListView<Item> fav2Results;

    @FXML private ImageView fav3Cover;
    @FXML private Label fav3Title;
    @FXML private Label fav3Artist;
    @FXML private Button fav3RemoveBtn;
    @FXML private HBox fav3SearchBox;
    @FXML private TextField fav3SearchField;
    @FXML private ListView<Item> fav3Results;

    @FXML private ImageView fav4Cover;
    @FXML private Label fav4Title;
    @FXML private Label fav4Artist;
    @FXML private Button fav4RemoveBtn;
    @FXML private HBox fav4SearchBox;
    @FXML private TextField fav4SearchField;
    @FXML private ListView<Item> fav4Results;

    private User user;
    private String newImagePath;
    private final Item[] currentFavorites = new Item[4];
    private final PauseTransition fav1Debounce = new PauseTransition(Duration.millis(600));
    private final PauseTransition fav2Debounce = new PauseTransition(Duration.millis(600));
    private final PauseTransition fav3Debounce = new PauseTransition(Duration.millis(600));
    private final PauseTransition fav4Debounce = new PauseTransition(Duration.millis(600));
    // pausa per non chiamare spotify troppo spesso

    private static final int BIO_MAX_LENGTH = 150;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Circle clip = new Circle(60, 60, 60);
        profileImagePreview.setClip(clip);
        
        bioField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > BIO_MAX_LENGTH) {
                bioField.setText(oldVal);
            } else {
                bioCounterLabel.setText(newVal.length() + "/" + BIO_MAX_LENGTH);
            }
        });

        setupListViewCellFactory(fav1Results);
        setupListViewCellFactory(fav2Results);
        setupListViewCellFactory(fav3Results);
        setupListViewCellFactory(fav4Results);

        // almeno due caratteri per la ricerca
        fav1SearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            fav1Debounce.stop();
            if (newVal == null || newVal.trim().length() < 2) {
                fav1Results.setVisible(false);
                fav1Results.setManaged(false);
                return;
            }
            fav1Debounce.setOnFinished(e -> searchFav(newVal, fav1Results));
            fav1Debounce.playFromStart();
        });
        fav1Results.setOnMouseClicked(e -> {
            Item selected = fav1Results.getSelectionModel().getSelectedItem();
            if (selected != null) {
                setFavoriteItem(0, selected);
            }
        });

        fav2SearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            fav2Debounce.stop();
            if (newVal == null || newVal.trim().length() < 2) {
                fav2Results.setVisible(false);
                fav2Results.setManaged(false);
                return;
            }
            fav2Debounce.setOnFinished(e -> searchFav(newVal, fav2Results));
            fav2Debounce.playFromStart();
        });
        fav2Results.setOnMouseClicked(e -> {
            Item selected = fav2Results.getSelectionModel().getSelectedItem();
            if (selected != null) {
                setFavoriteItem(1, selected);
            }
        });

        fav3SearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            fav3Debounce.stop();
            if (newVal == null || newVal.trim().length() < 2) {
                fav3Results.setVisible(false);
                fav3Results.setManaged(false);
                return;
            }
            fav3Debounce.setOnFinished(e -> searchFav(newVal, fav3Results));
            fav3Debounce.playFromStart();
        });
        fav3Results.setOnMouseClicked(e -> {
            Item selected = fav3Results.getSelectionModel().getSelectedItem();
            if (selected != null) {
                setFavoriteItem(2, selected);
            }
        });

        fav4SearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            fav4Debounce.stop();
            if (newVal == null || newVal.trim().length() < 2) {
                fav4Results.setVisible(false);
                fav4Results.setManaged(false);
                return;
            }
            fav4Debounce.setOnFinished(e -> searchFav(newVal, fav4Results));
            fav4Debounce.playFromStart();
        });
        fav4Results.setOnMouseClicked(e -> {
            Item selected = fav4Results.getSelectionModel().getSelectedItem();
            if (selected != null) {
                setFavoriteItem(3, selected);
            }
        });

        Platform.runLater(() -> {
            if (profileImagePreview.getScene() != null) {
                com.ale.tuney.ThemeManager.applyTheme(profileImagePreview.getScene().getRoot());
            }
        });
    }

    private void applyCoverImage(ImageView view, Image img) {
        if (img.getProgress() < 1.0 && img.isBackgroundLoading()) {
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0) {
                    setCoverViewport(view, img);
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
        if (w <= 0 || h <= 0) return;

        double size = Math.min(w, h);
        Rectangle2D viewport = new Rectangle2D(
                (w - size) / 2,
                (h - size) / 2,
                size,
                size
        );

        view.setViewport(viewport);
        view.setFitWidth(120);
        view.setFitHeight(120);
        view.setPreserveRatio(false);
    }

    public void setUser(User user) {
        this.user = user;

        nameField.setText(user.getName() != null ? user.getName() : "");
        usernameField.setText(user.getUsername() != null ? user.getUsername() : "");

        String bio = user.getBio() != null ? user.getBio() : "";
        bioField.setText(bio);
        bioCounterLabel.setText(bio.length() + "/" + BIO_MAX_LENGTH);

        try {
            Image img = com.ale.tuney.ImageLoader.loadProfilePicture(user.getProfileImageUrl());
            if (img != null) {
                applyCoverImage(profileImagePreview, img);
            }
        } catch (Exception ignored) {}

        try {
            List<Item> favorites = new UserFavoritesDAO().findFavoritesByUserId(user.getId());
            for (int i = 0; i < 4; i++) {
                if (i < favorites.size() && favorites.get(i) != null) {
                    setFavoriteItem(i, favorites.get(i));
                } else {
                    setFavoriteItem(i, null);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            for (int i = 0; i < 4; i++) {
                setFavoriteItem(i, null);
            }
        }
    }

    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Scegli un'immagine profilo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
                String base64Data = java.util.Base64.getEncoder().encodeToString(fileContent);
                String contentType = "image/png";
                String name = file.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                }
                newImagePath = "data:" + contentType + ";base64," + base64Data;

                Image img = com.ale.tuney.ImageLoader.load(newImagePath);
                if (img != null) {
                    applyCoverImage(profileImagePreview, img);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Errore durante la lettura dell'immagine.");
            }
        }
    }

    @FXML
    private void removeImage() {
        newImagePath = "";
        try {
            Image img = new Image(com.ale.tuney.ImageLoader.class.getResourceAsStream("/images/default-avatar.png"));
            applyCoverImage(profileImagePreview, img);
        } catch (Exception e) {
            e.printStackTrace();
            profileImagePreview.setImage(null);
        }
    }

    @FXML
    private void saveProfile(ActionEvent event) {
        String newName = nameField.getText().trim();
        String newBio = bioField.getText().trim();

        try {
            UserDAO userDAO = new UserDAO();

            user.setName(newName);
            user.setBio(newBio);

            if (newImagePath != null) {
                user.setProfileImageUrl(newImagePath);
            }

            userDAO.updateProfile(user);
            SessionManager.setCurrentUser(user);

            List<Item> favoritesList = new ArrayList<>();
            Collections.addAll(favoritesList, currentFavorites);

            List<Item> oldFavorites;
            try {
                oldFavorites = new UserFavoritesDAO().findFavoritesByUserId(user.getId());
            } catch (SQLException e) {
                oldFavorites = new ArrayList<>();
            }
            java.util.Set<String> oldIds = new java.util.HashSet<>();
            for (Item old : oldFavorites) {
                if (old != null) oldIds.add(old.getId());
            }

            new UserFavoritesDAO().saveFavorites(user.getId(), favoritesList);

            ActivityDAO activityDAO = new ActivityDAO();
            java.util.Set<String> newIds = new java.util.HashSet<>();
            for (Item item : favoritesList) {
                if (item != null) newIds.add(item.getId());
            }
            for (Item item : favoritesList) {
                if (item != null && !oldIds.contains(item.getId())) {
                    try {
                        activityDAO.save(user.getId(), "favorite", item.getId());
                    } catch (SQLException ignored) {}
                }
            }

            for (Item old : oldFavorites) {
                if (old != null && !newIds.contains(old.getId())) {
                    try {
                        activityDAO.deleteByTypeAndUserAndReferenceText(user.getId(), "favorite", old.getId());
                    } catch (SQLException ignored) {}
                }
            }

            backToProfile((Node) event.getSource());

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Errore durante il salvataggio. Riprova.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML
    private void cancelArrow(MouseEvent event) {
        backToProfile((Node) event.getSource());
    }

    @FXML
    private void cancelButton(ActionEvent event) {
        backToProfile((Node) event.getSource());
    }

    private void backToProfile(Node source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ale/tuney/profile-view.fxml"));
            BorderPane profileView = loader.load();
            ProfileController controller = loader.getController();
            controller.setUser(user);

            BorderPane currentRoot = (BorderPane) source.getScene().getRoot();
            currentRoot.setCenter(profileView.getCenter());

            Object mc = currentRoot.getProperties().get("mainController");
            if (mc instanceof MainController) {
                ((MainController) mc).loadNavbarImg();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void removeFav1() { setFavoriteItem(0, null); }
    @FXML private void removeFav2() { setFavoriteItem(1, null); }
    @FXML private void removeFav3() { setFavoriteItem(2, null); }
    @FXML private void removeFav4() { setFavoriteItem(3, null); }

    private void setupListViewCellFactory(ListView<Item> lv) {
        lv.setCellFactory(param -> new ListCell<>() {
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

                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    imageView.setImage(com.ale.tuney.ImageLoader.load(item.getImageUrl()));
                } else {
                    imageView.setImage(null);
                }

                titleLabel.setText(item.getTitle());
                titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");

                artistLabel.setText(String.join(", ", item.getArtists()));
                artistLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11;");

                typeLabel.setText(item instanceof Song ? "Brano" : "Album");
                typeLabel.setStyle("-fx-text-fill: #9b6fd4; -fx-font-size: 10;");

                setText(null);
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(container);
            }
        });
    }

    private void searchFav(String query, ListView<Item> resultsListView) {
        Task<List<Object>> task = new Task<>() {
            @Override
            protected List<Object> call() {
                return com.ale.tuney.api.SpotifyClient.search(query);
            }
        };

        task.setOnSucceeded(e -> {
            List<Object> rawResults = task.getValue();
            resultsListView.getItems().clear();
            for (Object obj : rawResults) {
                if (obj instanceof Item item) {
                    resultsListView.getItems().add(item);
                }
            }
            if (!resultsListView.getItems().isEmpty()) {
                resultsListView.setVisible(true);
                resultsListView.setManaged(true);
            } else {
                resultsListView.setVisible(false);
                resultsListView.setManaged(false);
            }
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            resultsListView.setVisible(false);
            resultsListView.setManaged(false);
        });

        new Thread(task).start();
    }

    private void setFavoriteItem(int index, Item item) {
        currentFavorites[index] = item;

        ImageView cover = getCoverForIndex(index);
        Label title = getTitleForIndex(index);
        Label artist = getArtistForIndex(index);
        Button removeBtn = getRemoveBtnForIndex(index);
        HBox searchBox = getSearchBoxForIndex(index);
        ListView<Item> results = getResultsForIndex(index);

        if (item != null) {
            title.setText(item.getTitle());
            artist.setText(String.join(", ", item.getArtists()) + " (" + (item instanceof Song ? "Brano" : "Album") + ")");
            artist.setTextFill(Color.web("#888888"));
            
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Image img = com.ale.tuney.ImageLoader.load(item.getImageUrl());
                cover.setImage(img);
            } else {
                cover.setImage(null);
            }

            removeBtn.setVisible(true);
            removeBtn.setManaged(true);
            searchBox.setVisible(false);
            searchBox.setManaged(false);
            results.setVisible(false);
            results.setManaged(false);
        } else {
            title.setText("Nessun elemento selezionato");
            artist.setText("Cerca per aggiungere");
            artist.setTextFill(Color.web("#888888"));
            cover.setImage(null);

            removeBtn.setVisible(false);
            removeBtn.setManaged(false);
            searchBox.setVisible(true);
            searchBox.setManaged(true);
            TextField searchField = getSearchFieldForIndex(index);
            searchField.clear();
            results.setVisible(false);
            results.setManaged(false);
        }
    }

    private ImageView getCoverForIndex(int idx) {
        return switch (idx) {
            case 0 -> fav1Cover;
            case 1 -> fav2Cover;
            case 2 -> fav3Cover;
            case 3 -> fav4Cover;
            default -> null;
        };
    }
    private Label getTitleForIndex(int idx) {
        return switch (idx) {
            case 0 -> fav1Title;
            case 1 -> fav2Title;
            case 2 -> fav3Title;
            case 3 -> fav4Title;
            default -> null;
        };
    }
    private Label getArtistForIndex(int idx) {
        return switch (idx) {
            case 0 -> fav1Artist;
            case 1 -> fav2Artist;
            case 2 -> fav3Artist;
            case 3 -> fav4Artist;
            default -> null;
        };
    }
    private Button getRemoveBtnForIndex(int idx) {
        return switch (idx) {
            case 0 -> fav1RemoveBtn;
            case 1 -> fav2RemoveBtn;
            case 2 -> fav3RemoveBtn;
            case 3 -> fav4RemoveBtn;
            default -> null;
        };
    }
    private HBox getSearchBoxForIndex(int idx) {
        return switch (idx) {
            case 0 -> fav1SearchBox;
            case 1 -> fav2SearchBox;
            case 2 -> fav3SearchBox;
            case 3 -> fav4SearchBox;
            default -> null;
        };
    }
    private TextField getSearchFieldForIndex(int idx) {
        return switch (idx) {
            case 0 -> fav1SearchField;
            case 1 -> fav2SearchField;
            case 2 -> fav3SearchField;
            case 3 -> fav4SearchField;
            default -> null;
        };
    }
    private ListView<Item> getResultsForIndex(int idx) {
        return switch (idx) {
            case 0 -> fav1Results;
            case 1 -> fav2Results;
            case 2 -> fav3Results;
            case 3 -> fav4Results;
            default -> null;
        };
    }
}
