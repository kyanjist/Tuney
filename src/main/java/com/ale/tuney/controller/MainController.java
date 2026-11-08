package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.TuneyApplication;
import com.ale.tuney.ThemeManager;
import javafx.scene.Parent;
import com.ale.tuney.dao.ReviewDAO;
import com.ale.tuney.model.Review;
import com.ale.tuney.model.Activity;
import com.ale.tuney.model.User;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.sql.SQLException;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.Separator;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private BorderPane root;
    @FXML
    private TextField searchField;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane feedPane;
    @FXML
    private VBox activityPane;
    @FXML
    private VBox menuPane;
    @FXML
    private VBox rightPane;
    @FXML
    private javafx.scene.Group leftToggleIcon;
    @FXML
    private HBox menuToggleRow;
    @FXML
    private HBox rightToggleRow;
    @FXML
    private Label menuTitle;
    @FXML
    private Label rightTitle;
    @FXML
    private ImageView navProfilePicture;
    @FXML
    private Circle notificationDot;
    @FXML
    private HBox seguitiTab;
    @FXML
    private HBox perTeTab;
    @FXML
    private Rectangle followingTabUnderline;
    @FXML
    private Rectangle forYouTabUnderline;
    @FXML
    private VBox feedContainer;

    private boolean menuOpen = true;
    private boolean rightOpen = true;
    private Pane cardOverlayPane;
    private Node[] currentOverlayRef = null;
    private Animation[] currentAnimRef = null;
    private String activeTab = "PerTe";
    private double xOffset = 0;
    private double yOffset = 0;

    private Node homeCenterNode;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        homeCenterNode = root.getCenter();
        root.getProperties().put("mainController", this);
        setupCardOverlay();
        setActiveMenu("Home");

        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> adjustGridWidth());
        scrollPane.widthProperty().addListener((obs, oldVal, newVal) -> adjustGridWidth());

        scrollPane.layoutBoundsProperty().addListener((obs, old, bounds) -> {
            Rectangle clip = new Rectangle(bounds.getWidth(), bounds.getHeight());
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            scrollPane.setClip(clip);
        });

        Circle navClip = new Circle(20, 20, 20);
        navProfilePicture.setClip(navClip);

        loadNavbarImg();

        loadForYou();
        loadActivity();
        updateNotifsBadge();

        // forza il calcolo del layout prima che la scena venga mostrata
        prepareLayoutSync();

        Platform.runLater(() -> {
            if (root.getScene() != null) {
                ThemeManager.applyTheme(root.getScene().getRoot());
            }
            scrollPane.lookupAll("*").forEach(n -> {
                if (n.getStyleClass().contains("corner")) {
                    n.setStyle("-fx-background-color: #090909;");
                }
            });
            adjustGridWidth();
        });
    }

    public void prepareLayoutSync() {
        root.applyCss();
        root.layout();
        adjustGridWidth();
        root.applyCss();
        root.layout();
    }

    private void setupCardOverlay() {
        if (!(scrollPane.getParent() instanceof VBox parentVBox)) {
            System.err.println("setupCardOverlay: genitore inatteso, overlay non attivato");
            return;
        }

        int index = parentVBox.getChildren().indexOf(scrollPane);
        Priority vgrow = VBox.getVgrow(scrollPane);

        cardOverlayPane = new Pane();
        cardOverlayPane.setPickOnBounds(false);

        StackPane wrapper = new StackPane(scrollPane, cardOverlayPane);
        cardOverlayPane.prefWidthProperty().bind(wrapper.widthProperty());
        cardOverlayPane.prefHeightProperty().bind(wrapper.heightProperty());

        parentVBox.getChildren().remove(scrollPane);
        parentVBox.getChildren().add(index, wrapper);

        if (vgrow != null) {
            VBox.setVgrow(wrapper, vgrow);
        }
    }

    public void loadNavbarImg() {
        com.ale.tuney.ImageLoader.loadNavbarImage(navProfilePicture);
    }

    public void setActiveMenu(String menuItemName) {
        if (menuPane == null)
            return;
        for (javafx.scene.Node node : menuPane.getChildren()) {
            if (node instanceof Label label) {
                String txt = label.getText();
                String name = (txt != null && !txt.isEmpty())
                        ? txt
                        : (label.getUserData() instanceof String s ? s : "");

                if (List.of("Home", "Cerca", "Salvati", "Profilo", "Impostazioni", "Log-out", "Nuova Recensione")
                        .contains(name)) {
                    boolean active = name.equalsIgnoreCase(menuItemName);

                    label.setStyle(menuOpen ? "" : "-fx-max-width: 34px;");
                    if (active) {
                        if (!label.getStyleClass().contains("menu-item-active")) {
                            label.getStyleClass().add("menu-item-active");
                        }
                    } else {
                        label.getStyleClass().remove("menu-item-active");
                    }

                    colorIcon(label, active ? "#9b6fd4" : "white");
                }
            }
        }
    }

    private void colorIcon(Label label, String colorHex) {
        if (label.getGraphic() == null)
            return;
        colorSVG(label.getGraphic(), Color.web(colorHex));
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

    @FXML
    private void toggleMenu() {
        if (menuOpen) {
            menuPane.getChildren().forEach(n -> {
                if (n instanceof Label label && label.getGraphic() != null
                        && label.getStyleClass().contains("menu-item")) {
                    if (label.getUserData() == null) {
                        label.setUserData(label.getText());
                    }
                    label.setText("");
                    label.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                    label.setVisible(true);
                    label.setManaged(true);
                    label.setAlignment(Pos.CENTER);
                    label.setMinWidth(34);
                    label.setPrefWidth(34);
                    label.setMaxWidth(34);
                    label.setStyle("-fx-max-width: 34px;");
                    label.setMinHeight(34);
                    label.setPrefHeight(34);
                    label.setMaxHeight(34);
                    label.setGraphicTextGap(0);
                    label.setPadding(new Insets(0));
                } else {
                    n.setVisible(false);
                    n.setManaged(false);
                }
            });
            if (menuToggleRow != null) {
                menuToggleRow.setVisible(true);
                menuToggleRow.setManaged(true);
                menuToggleRow.setAlignment(Pos.CENTER);
                if (menuToggleRow.getChildren().size() > 1) {
                    menuToggleRow.getChildren().get(1).setVisible(false);
                    menuToggleRow.getChildren().get(1).setManaged(false);
                }
            }
            if (menuTitle != null) {
                menuTitle.setVisible(false);
                menuTitle.setManaged(false);
            }
            if (leftToggleIcon != null) {
                leftToggleIcon.setRotate(0);
            }

            Separator miniSep = new Separator();
            miniSep.setId("mini-sep-chiuso");
            miniSep.setPrefWidth(30);
            miniSep.setMaxWidth(30);
            miniSep.setPadding(new Insets(10, 0, 10, 0));

            javafx.scene.shape.SVGPath matita = new javafx.scene.shape.SVGPath();
            matita.setContent(
                    "M216-432h51l339-339-51-51-339 339v51Zm-80 80v-170l442-442q13-13 31-13t32 14l84 84q14 13 14 32t-14 32L258-352H136Zm560-503-56-56 56 56Zm-141 85-25-26 51 51-26-25Z");
            matita.setFill(javafx.scene.paint.Color.WHITE);
            matita.setScaleX(0.02);
            matita.setScaleY(0.02);

            Label pencilIcon = new Label();
            pencilIcon.setId("icona-matita-chiuso");
            pencilIcon.setGraphic(matita);
            pencilIcon.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            pencilIcon.setAlignment(Pos.CENTER);
            pencilIcon.setMinWidth(34);
            pencilIcon.setPrefWidth(34);
            pencilIcon.setMaxWidth(34);
            pencilIcon.setMinHeight(34);
            pencilIcon.setPrefHeight(34);
            pencilIcon.setMaxHeight(34);
            pencilIcon.setPadding(new Insets(6));
            pencilIcon.getStyleClass().add("menu-item");
            pencilIcon.setStyle("-fx-cursor: hand; -fx-max-width: 34px;");
            pencilIcon.setOnMouseClicked(e -> openReviewPage());

            menuPane.getChildren().add(miniSep);
            menuPane.getChildren().add(pencilIcon);

            menuPane.setPadding(new Insets(16, 0, 16, 0));
            menuPane.setPrefWidth(50);
            menuPane.setAlignment(Pos.TOP_CENTER);
        } else {
            menuPane.getChildren()
                    .removeIf(n -> "mini-sep-chiuso".equals(n.getId()) || "icona-matita-chiuso".equals(n.getId()));

            menuPane.getChildren().forEach(n -> {
                n.setVisible(true);
                n.setManaged(true);
                if (n instanceof Label label && label.getGraphic() != null
                        && label.getStyleClass().contains("menu-item")) {
                    if (label.getUserData() != null) {
                        label.setText((String) label.getUserData());
                        label.setUserData(null);
                    }
                    label.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                    label.setAlignment(Pos.CENTER_LEFT);
                    label.setGraphicTextGap(10);
                    label.setMinHeight(Region.USE_COMPUTED_SIZE);
                    label.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    label.setMaxHeight(Region.USE_COMPUTED_SIZE);
                    label.setPadding(new Insets(6, 6, 6, 6));
                    label.setMinWidth(Region.USE_COMPUTED_SIZE);
                    label.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    label.setMaxWidth(Double.MAX_VALUE);
                    label.setStyle("");
                }
            });
            if (menuToggleRow != null) {
                menuToggleRow.setAlignment(Pos.CENTER_LEFT);
                if (menuToggleRow.getChildren().size() > 1) {
                    menuToggleRow.getChildren().get(1).setVisible(true);
                    menuToggleRow.getChildren().get(1).setManaged(true);
                }
            }
            if (menuTitle != null) {
                menuTitle.setVisible(true);
                menuTitle.setManaged(true);
            }
            if (leftToggleIcon != null) {
                leftToggleIcon.setRotate(0);
            }
            menuPane.setPadding(new Insets(16, 16, 16, 16));
            menuPane.setPrefWidth(251);
            menuPane.setAlignment(Pos.TOP_LEFT);
        }
        menuOpen = !menuOpen;
    }

    @FXML
    private void toggleRightPane() {
        if (rightOpen) {
            rightPane.getChildren().forEach(n -> {
                n.setVisible(false);
                n.setManaged(false);
            });
            rightToggleRow.setVisible(true);
            rightToggleRow.setManaged(true);
            rightTitle.setVisible(false);
            rightTitle.setManaged(false);
            rightPane.setPrefWidth(50);
        } else {
            rightPane.getChildren().forEach(n -> {
                n.setVisible(true);
                n.setManaged(true);
            });
            rightTitle.setVisible(true);
            rightTitle.setManaged(true);
            rightPane.setPrefWidth(251);
        }
        rightOpen = !rightOpen;
    }

    @FXML
    public void loadFollowing() {
        try {
            activeTab = "Seguiti";
            ReviewDAO dao = new ReviewDAO();
            int userId = SessionManager.getCurrentUser().getId();
            List<Review> reviews = dao.findByFollowed(userId);

            feedPane.getChildren().clear();
            feedContainer.getChildren().removeIf(n -> "placeholder-seguiti".equals(n.getId()));

            if (reviews.isEmpty()) {
                com.ale.tuney.dao.FollowDAO followDAO = new com.ale.tuney.dao.FollowDAO();
                int followingCount = followDAO.getFollowingCount(userId);

                if (followingCount == 0) {
                    feedPane.setVisible(false);
                    feedPane.setManaged(false);

                    feedContainer.setAlignment(Pos.CENTER);
                    feedContainer.minHeightProperty().bind(scrollPane.heightProperty());

                    VBox placeholder = createFollowingPlaceholder();
                    placeholder.setId("placeholder-seguiti");
                    feedContainer.getChildren().add(placeholder);
                } else {
                    feedPane.setVisible(true);
                    feedPane.setManaged(true);
                    feedContainer.setAlignment(Pos.TOP_CENTER);
                    feedContainer.minHeightProperty().unbind();
                    feedContainer.setMinHeight(Region.USE_COMPUTED_SIZE);
                }
            } else {
                feedPane.setVisible(true);
                feedPane.setManaged(true);
                feedContainer.setAlignment(Pos.TOP_CENTER);
                feedContainer.minHeightProperty().unbind();
                feedContainer.setMinHeight(Region.USE_COMPUTED_SIZE);
                showCard(reviews);

                Platform.runLater(() -> {
                    scrollPane.applyCss();
                    scrollPane.layout();
                    adjustGridWidth();
                });
            }

            setActiveTab(followingTabUnderline);
        } catch (Exception e) {
            System.err.println("Errore in loadFollowing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void loadForYou() {
        try {
            activeTab = "PerTe";
            feedContainer.getChildren().removeIf(n -> "placeholder-seguiti".equals(n.getId()));
            feedPane.setVisible(true);
            feedPane.setManaged(true);
            feedContainer.setAlignment(Pos.TOP_CENTER);
            feedContainer.minHeightProperty().unbind();
            feedContainer.setMinHeight(Region.USE_COMPUTED_SIZE);

            ReviewDAO dao = new ReviewDAO();
            List<Review> reviews = dao.findAll();
            showCard(reviews);
            setActiveTab(forYouTabUnderline);

            Platform.runLater(() -> {
                scrollPane.applyCss();
                scrollPane.layout();
                adjustGridWidth();
            });
        } catch (Exception e) {
            System.err.println("Errore in loadForYou: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadActiveTab() {
        if ("Seguiti".equals(activeTab)) {
            loadFollowing();
        } else {
            loadForYou();
        }
    }

    private VBox createFollowingPlaceholder() {
        VBox placeholder = new VBox(16);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(40, 16, 16, 16));
        placeholder.setTranslateY(-40);

        try {
            Image img = new Image(
                    getClass().getResourceAsStream("/images/user-regular.png"));
            ImageView icon = new ImageView(img);
            icon.setFitWidth(110);
            icon.setFitHeight(136);
            icon.setPreserveRatio(true);
            icon.setOpacity(0.4);
            placeholder.getChildren().add(icon);
        } catch (Exception e) {
            System.err.println("Impossibile caricare user-regular.png: " + e.getMessage());
        }

        Label msg = new Label("Trova persone da seguire per leggere le loro recensioni!");
        msg.setTextFill(Color.GRAY);
        msg.setFont(new Font(16));
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msg.setMaxWidth(400);

        placeholder.getChildren().add(msg);
        return placeholder;
    }

    @FXML
    void openReviewPage() {
        setActiveMenu("Nuova Recensione");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(TuneyApplication.class.getResource("review-view.fxml"));
            BorderPane loadedRoot = fxmlLoader.load();
            root.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore apertura recensione");
            alert.setHeaderText("Impossibile aprire la schermata");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            alert.showAndWait();
        }
    }

    public void loadActivity() {
        com.ale.tuney.ActivityLoader.loadActivity(activityPane);
    }

    private void showCard(List<Review> reviews) {
        feedPane.getChildren().clear();
        for (Review review : reviews) {
            feedPane.getChildren().add(createCard(review));
        }
    }

    private VBox createCard(Review review) {
        VBox card = createContentCard(review, false);

        final Node[] overlayRef = new Node[1];
        final Animation[] animRef = new Animation[1];

        card.setOnMouseEntered(e -> showExpandedCard(card, review, overlayRef, animRef));

        card.setOnMouseClicked(e -> openReviewDetail(review));

        return card;
    }

    private VBox createContentCard(Review review, boolean expanded) {
        double width = expanded ? 320 : 280;
        double coverSize = expanded ? 112 : 100;
        double horizontalPadding = (expanded ? 16 : 12) * 2;
        double infoWidth = width - horizontalPadding - coverSize - 8; // 8 = padding sinistro di "info"

        VBox card = new VBox();
        card.setPrefWidth(width);
        card.setMaxWidth(width);
        card.setMinHeight(180);
        if (expanded) {
            card.setMaxHeight(Region.USE_COMPUTED_SIZE);
        } else {
            card.setPrefHeight(180);
            card.setMaxHeight(180);
        }
        card.setStyle(expanded
                ? "-fx-background-color: #1a1a1a; -fx-background-radius: 16; -fx-border-color: rgba(155,111,212,0.35); -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand;"
                : "-fx-background-color: #090909; -fx-background-radius: 16; -fx-cursor: hand;");
        card.setPadding(new Insets(expanded ? 12 : 8, expanded ? 16 : 12, expanded ? 12 : 8, expanded ? 16 : 12));
        if (expanded) {
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.7));
            shadow.setRadius(40);
            shadow.setOffsetY(16);
            shadow.setSpread(0.08);
            card.setEffect(shadow);
        }

        Label username = new Label("@" + review.getAuthor().getUsername());
        username.setTextFill(Color.web("#ffb3d9"));
        username.setStyle("-fx-font-weight: bold; -fx-font-size: " + (expanded ? 18 : 16) + "px;");
        username.setPadding(new Insets(3, 0, 3, 0));
        username.getStyleClass().add("username-link");
        username.setOnMouseClicked(e -> {
            e.consume();
            openUserProfile(review.getAuthor());
        });

        HBox hbox = new HBox();

        ImageView cover = new ImageView();
        cover.setFitWidth(coverSize);
        cover.setFitHeight(coverSize);
        cover.setPreserveRatio(true);

        VBox info = new VBox(4);
        info.setPadding(new Insets(0, 0, 0, 8));

        String stars = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
        Label starsLabel = new Label(stars);
        starsLabel.setTextFill(Color.web("#ffe11f"));
        starsLabel.setFont(new Font(expanded ? 24 : 22));

        Label title = new Label();
        title.setTextFill(Color.WHITE);
        title.setFont(new Font(expanded ? 16 : 14));
        title.setWrapText(true);
        title.setMaxWidth(infoWidth);

        Label artist = new Label();
        artist.setTextFill(Color.LIGHTGRAY);
        artist.setFont(new Font(expanded ? 14 : 12));
        artist.setMaxWidth(infoWidth);

        if (review.getTarget() != null) {
            if (review.getTarget().getImageUrl() != null) {
                cover.setImage(com.ale.tuney.ImageLoader.load(review.getTarget().getImageUrl()));
            }
            title.setText(truncateByLines(review.getTarget().getTitle(), infoWidth, title.getFont(), 2));
            String artists = String.join(", ", review.getTarget().getArtists());
            artist.setText(artists);
        } else {
            title.setText("Traccia non disponibile (Rate Limit)");
            artist.setText("Riprova tra poco");
        }

        info.getChildren().addAll(starsLabel, title, artist);
        hbox.getChildren().addAll(cover, info);

        Label comment = new Label();
        comment.setTextFill(Color.WHITE);
        comment.setFont(new Font(expanded ? 15 : 14));
        comment.setWrapText(expanded);
        comment.setMaxWidth(expanded ? width - 24 : 264);
        comment.setPadding(new Insets(6, 0, 6, 0));
        if (expanded) {
            comment.setText(review.getText());
        } else {
            comment.setText(truncateByLines(review.getText(), 264, comment.getFont(), 2));
            comment.setWrapText(true);
        }

        card.getChildren().addAll(username, hbox, comment);
        return card;
    }

    private String truncateByLines(String text, double maxWidth, Font font, int maxLines) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Text textMeasurer = new Text();
        textMeasurer.setFont(font);
        textMeasurer.setWrappingWidth(maxWidth);

        Text singleLine = new Text("Ag");
        singleLine.setFont(font);
        double lineHeight = singleLine.getLayoutBounds().getHeight();
        double maxHeight = lineHeight * maxLines + 2;

        textMeasurer.setText(text);
        if (textMeasurer.getLayoutBounds().getHeight() <= maxHeight) {
            return text;
        }

        String current = text;
        while (!current.isEmpty()) {
            current = current.substring(0, current.length() - 1);
            textMeasurer.setText(current.stripTrailing() + "…");
            if (textMeasurer.getLayoutBounds().getHeight() <= maxHeight) {
                return current.stripTrailing() + "…";
            }
        }
        return text;
    }

    private void showExpandedCard(VBox originalCard, Review review, Node[] overlayRef, Animation[] animRef) {
        if (cardOverlayPane == null || originalCard.getScene() == null)
            return;

        if (currentOverlayRef != null && currentOverlayRef != overlayRef && currentOverlayRef[0] != null) {
            cardOverlayPane.getChildren().remove(currentOverlayRef[0]);
            currentOverlayRef[0] = null;
            if (currentAnimRef != null && currentAnimRef[0] != null) {
                currentAnimRef[0].stop();
                currentAnimRef[0] = null;
            }
        }

        if (animRef[0] != null) {
            animRef[0].stop();
            animRef[0] = null;
        }

        if (overlayRef[0] == null) {
            VBox expanded = createContentCard(review, true);

            expanded.setOnMouseExited(e -> hideExpandedCard(overlayRef, animRef));
            expanded.setOnMouseClicked(e -> openReviewDetail(review));

            expanded.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
                double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                double scrollableHeight = contentHeight - viewportHeight;

                if (scrollableHeight > 0) {
                    double oldV = scrollPane.getVvalue();
                    double delta = e.getDeltaY() / scrollableHeight;
                    double newV = Math.max(0, Math.min(1, oldV - delta));

                    if (newV != oldV) {
                        // sposta la card espansa insieme al feed che scorre
                        expanded.setLayoutY(expanded.getLayoutY() + e.getDeltaY());
                        scrollPane.setVvalue(newV);
                    }
                }
                e.consume();
            });

            expanded.applyCss();
            expanded.layout();
            double expandedWidth = expanded.prefWidth(-1);
            expanded.prefHeight(expandedWidth);

            Bounds inOverlay = cardOverlayPane.sceneToLocal(originalCard.localToScene(originalCard.getBoundsInLocal()));
            double centerX = inOverlay.getMinX() + inOverlay.getWidth() / 2;

            expanded.setLayoutX(centerX - expandedWidth / 2);
            expanded.setLayoutY(inOverlay.getMinY());

            expanded.setOpacity(0);
            expanded.setScaleX(0.94);
            expanded.setScaleY(0.94);

            cardOverlayPane.getChildren().add(expanded);
            overlayRef[0] = expanded;
            currentOverlayRef = overlayRef;
            currentAnimRef = animRef;
        }

        Node expanded = overlayRef[0];

        FadeTransition fade = new FadeTransition(Duration.millis(160), expanded);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(160), expanded);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition show = new ParallelTransition(fade, scale);
        show.play();
        animRef[0] = show;

        currentOverlayRef = overlayRef;
        currentAnimRef = animRef;
    }

    private void hideExpandedCard(Node[] overlayRef, Animation[] animRef) {
        if (overlayRef[0] == null)
            return;

        if (animRef[0] != null) {
            animRef[0].stop();
            animRef[0] = null;
        }

        Node expanded = overlayRef[0];

        FadeTransition fade = new FadeTransition(Duration.millis(130), expanded);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scale = new ScaleTransition(Duration.millis(130), expanded);
        scale.setToX(0.94);
        scale.setToY(0.94);
        scale.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition hide = new ParallelTransition(fade, scale);
        hide.setOnFinished(e -> {
            cardOverlayPane.getChildren().remove(expanded);
            overlayRef[0] = null;
        });
        hide.play();
        animRef[0] = hide;
    }

    public void openUserProfile(com.ale.tuney.model.User user) {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("profile-view.fxml"));
            BorderPane profileView = loader.load();
            ProfileController ctrl = loader.getController();
            ctrl.setUser(user);

            BorderPane currentRoot = root;
            currentRoot.setCenter(profileView.getCenter());

            setActiveMenu("Profilo");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openReviewDetail(Review review) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    TuneyApplication.class.getResource("review-detail-view.fxml"));
            BorderPane loadedRoot = loader.load();
            ReviewDetailController controller = loader.getController();
            controller.setReview(review);
            controller.setPreviousCenterNode(root.getCenter());
            root.setCenter(loadedRoot.getCenter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void makeDraggable(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void moveWindow(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (ThemeManager.isWindowMaximized(stage)) {
            double ratioX = event.getSceneX() / stage.getWidth();
            ThemeManager.toggleMaximize(stage);
            xOffset = stage.getWidth() * ratioX;
        }
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    private void onSearchClick() {
        if (searchField == null)
            return;
        String query = searchField.getText().trim();
        if (query.isEmpty())
            return;

        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("search-view.fxml"));
            BorderPane searchView = loader.load();
            SearchController controller = loader.getController();

            BorderPane currentRoot = root;
            currentRoot.setCenter(searchView.getCenter());
            setActiveMenu("Cerca");

            Platform.runLater(() -> {
                controller.getSearchField().setText(query);
                try {
                    controller.triggerSearch();
                } catch (Exception e) {

                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeApp(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void maximizeRestoreWindow(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ThemeManager.toggleMaximize(stage);
    }

    @FXML
    private void minimizeWindow(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void openProfile(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("profile-view.fxml"));
            BorderPane profileView = loader.load();
            ProfileController controller = loader.getController();
            controller.setUser(SessionManager.getCurrentUser());

            BorderPane.setMargin(profileView, new Insets(0));

            BorderPane currentRoot = (BorderPane) ((Node) event.getSource()).getScene().getRoot();
            currentRoot.setCenter(profileView.getCenter());

            setActiveMenu("Profilo");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void backToHome(MouseEvent event) {
        if (homeCenterNode == null)
            return;

        BorderPane currentRoot = (BorderPane) ((Node) event.getSource()).getScene().getRoot();
        currentRoot.setCenter(homeCenterNode);

        loadForYou();
        loadActivity();

        setActiveMenu("Home");

        Platform.runLater(() -> adjustGridWidth());
    }

    @FXML
    private void openNotifs() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null)
            return;

        Stage ownerStage = (Stage) root.getScene().getWindow();
        Stage popup = new Stage();
        popup.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        popup.initOwner(ownerStage);
        popup.setTitle("Notifiche");

        Label headerLabel = new Label("Notifiche");
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

        try {
            com.ale.tuney.dao.ActivityDAO activityDAO = new com.ale.tuney.dao.ActivityDAO();
            List<Activity> notifications = activityDAO.findNotificationsForUser(currentUser.getId());

            if (notifications.isEmpty()) {
                Label empty = new Label("Nessuna notifica");
                empty.setTextFill(Color.GRAY);
                empty.setFont(new Font(14));
                empty.setPadding(new Insets(16, 20, 16, 20));
                lista.getChildren().add(empty);
            } else {
                for (Activity activity : notifications) {
                    HBox row = createNotifRow(activity, popup);
                    lista.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errLabel = new Label("Errore nel caricamento delle notifiche");
            errLabel.setTextFill(Color.RED);
            errLabel.setFont(new Font(14));
            errLabel.setPadding(new Insets(16, 20, 16, 20));
            lista.getChildren().add(errLabel);
        }

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");
        scroll.setPrefHeight(420);
        scroll.setMinHeight(420);
        scroll.setMaxHeight(420);

        VBox popupRoot = new VBox(header, scroll);
        popupRoot.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 16;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 24, 0, 0, 8);");
        popupRoot.setPrefWidth(340);
        popupRoot.setMaxWidth(340);
        popupRoot.setMaxHeight(Region.USE_COMPUTED_SIZE);

        try {
            com.ale.tuney.dao.ActivityDAO activityDAO = new com.ale.tuney.dao.ActivityDAO();
            activityDAO.markNotificationsAsRead(currentUser.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (notificationDot != null) {
            notificationDot.setVisible(false);
            notificationDot.setManaged(false);
        }

        StackPane overlay = new StackPane(popupRoot);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay)
                popup.close();
        });

        Scene scene = new Scene(overlay, 600, 500);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/com/ale/tuney/style.css").toExternalForm());

        popup.setScene(scene);
        popup.setX(ownerStage.getX() + (ownerStage.getWidth() - scene.getWidth()) / 2);
        popup.setY(ownerStage.getY() + (ownerStage.getHeight() - scene.getHeight()) / 2);

        ThemeManager.applyTheme(overlay);
        popup.show();
    }

    private void updateNotifsBadge() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || notificationDot == null)
            return;
        try {
            com.ale.tuney.dao.ActivityDAO activityDAO = new com.ale.tuney.dao.ActivityDAO();
            boolean hasUnread = activityDAO.hasUnreadNotifications(currentUser.getId());
            notificationDot.setVisible(hasUnread);
            notificationDot.setManaged(hasUnread);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createNotifRow(Activity activity, Stage popup) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 20, 8, 20));
        row.setStyle("-fx-border-color: transparent transparent #2a2a2a transparent; -fx-border-width: 0 0 1 0;");

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
        flow.setStyle("-fx-font-size: 14.5px;");
        
        VBox flowContainer = new VBox(flow);
        flowContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(flowContainer, Priority.ALWAYS);

        Text userText = new Text("@" + activity.getUser().getUsername());
        userText.setFill(Color.web("#ffb3d9"));
        userText.getStyleClass().add("username-link");
        userText.setStyle("-fx-font-weight: bold;");
        userText.setOnMouseClicked(e -> {
            popup.close();
            openUserProfile(activity.getUser());
        });
        flow.getChildren().add(userText);

        if ("follow".equals(activity.getType())) {
            Text actionText = new Text(" ha iniziato a seguirti.");
            actionText.setFill(Color.WHITE);
            flow.getChildren().add(actionText);
        } else if ("like".equals(activity.getType())) {
            Text actionText = new Text(" ha messo \"Mi piace\" alla tua recensione di ");
            actionText.setFill(Color.WHITE);
            flow.getChildren().add(actionText);

            Review review = activity.getReview();
            if (review != null && review.getTarget() != null) {
                String title = review.getTarget().getTitle();
                Text itemText = new Text("\"" + title + "\"");
                itemText.setFill(Color.web("#9b6fd4"));
                itemText.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
                itemText.setOnMouseClicked(e -> {
                    popup.close();
                    openReviewDetail(review);
                });
                flow.getChildren().add(itemText);
            } else {
                Text genericText = new Text("una tua recensione.");
                genericText.setFill(Color.WHITE);
                flow.getChildren().add(genericText);
            }
        } else if ("comment".equals(activity.getType())) {
            Text actionText = new Text(" ha commentato la tua recensione su ");
            actionText.setFill(Color.WHITE);
            flow.getChildren().add(actionText);

            Review review = activity.getReview();
            if (review != null && review.getTarget() != null) {
                String title = review.getTarget().getTitle();
                Text itemText = new Text("\"" + title + "\"");
                itemText.setFill(Color.web("#9b6fd4"));
                itemText.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
                itemText.setOnMouseClicked(e -> {
                    popup.close();
                    openReviewDetail(review);
                });
                flow.getChildren().add(itemText);
            } else {
                Text genericText = new Text("una tua recensione.");
                genericText.setFill(Color.WHITE);
                flow.getChildren().add(genericText);
            }
        }

        Label timeLabel = new Label(timeAgo(activity.getCreatedAt()));
        timeLabel.setTextFill(Color.GRAY);
        timeLabel.setFont(new Font(13));
        timeLabel.setMinWidth(Region.USE_PREF_SIZE);

        row.getChildren().addAll(avatarStack, flowContainer, timeLabel);
        return row;
    }

    private String timeAgo(String createdAt) {
        if (createdAt == null)
            return "";
        try {
            java.time.LocalDateTime data = java.time.LocalDateTime.parse(
                    createdAt.replace(" ", "T"));
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
    private void openSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("search-view.fxml"));
            BorderPane loadedRoot = loader.load();
            root.setCenter(loadedRoot.getCenter());
            setActiveMenu("Cerca");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openSaved() {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("saved-items-view.fxml"));
            BorderPane loadedRoot = loader.load();
            root.setCenter(loadedRoot.getCenter());
            setActiveMenu("Salvati");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("settings-view.fxml"));
            BorderPane loadedRoot = loader.load();
            root.setCenter(loadedRoot.getCenter());
            setActiveMenu("Impostazioni");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveTab(Rectangle tabUnderline) {
        followingTabUnderline.setFill(Color.TRANSPARENT);
        forYouTabUnderline.setFill(Color.TRANSPARENT);
        colorTabText(seguitiTab, Color.WHITE);
        colorTabText(perTeTab, Color.WHITE);

        if (tabUnderline != null) {
            tabUnderline.setFill(Color.web("#9b6fd4"));
        }
        if (tabUnderline == followingTabUnderline) {
            colorTabText(seguitiTab, Color.web("#9b6fd4"));
        } else if (tabUnderline == forYouTabUnderline) {
            colorTabText(perTeTab, Color.web("#9b6fd4"));
        }
    }

    private void colorTabText(javafx.scene.Node node, Color colore) {
        if (node instanceof Label label) {
            label.setTextFill(colore);
        } else if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                colorTabText(child, colore);
            }
        }
    }

    @FXML
    private void logout(javafx.event.Event event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Stage popup = new Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        popup.initOwner(ownerStage);
        popup.setTitle("Log-out");

        Label titleLbl = new Label("Log-out");
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setFont(new Font("System Bold", 18));

        Label msgLbl = new Label("Sei sicuro di voler uscire da Tuney?");
        msgLbl.setTextFill(Color.web("#b3b3b3"));
        msgLbl.setFont(new Font(14));
        msgLbl.setWrapText(true);

        Button btnCancel = new Button("Annulla");
        btnCancel.getStyleClass().add("dialog-btn-cancel");
        btnCancel.setMinWidth(100);
        btnCancel.setOnAction(e -> popup.close());

        Button btnConfirm = new Button("Conferma");
        btnConfirm.getStyleClass().add("dialog-btn-confirm");
        btnConfirm.setMinWidth(100);
        btnConfirm.setOnAction(e -> {
            popup.close();
            com.ale.tuney.SessionManager.logout();
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/ale/tuney/tuney-view.fxml"));
                Parent loadedRoot = loader.load();
                ThemeManager.applyTheme(loadedRoot);
                ownerStage.setScene(new Scene(loadedRoot));

                if (ThemeManager.isWindowMaximized(ownerStage)) {
                    ThemeManager.toggleMaximize(ownerStage);
                }
                ownerStage.setWidth(1280);
                ownerStage.setHeight(800);
                ownerStage.centerOnScreen();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox buttons = new HBox(12, btnCancel, btnConfirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 0, 0, 0));

        VBox card = new VBox(10, titleLbl, msgLbl, buttons);
        card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 24, 0, 0, 8); -fx-padding: 20;");
        card.setPrefWidth(350);
        card.setMaxWidth(350);

        StackPane overlay = new StackPane(card);
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
    }
}
