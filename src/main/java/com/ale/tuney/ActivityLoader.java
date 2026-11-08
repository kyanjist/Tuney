package com.ale.tuney;

import com.ale.tuney.dao.ActivityDAO;
import com.ale.tuney.dao.FollowDAO;
import com.ale.tuney.model.Activity;
import com.ale.tuney.model.Review;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.fxml.FXMLLoader;

import java.util.List;

public class ActivityLoader {

    private static final String ITEM_COLOR = "#9b6fd4";

    public static void loadActivity(VBox activityPane) {
        loadActivity(activityPane, true);
    }

    public static void loadActivity(VBox activityPane, boolean newestFirst) {
        if (activityPane == null)
            return;

        int userId = SessionManager.getCurrentUser().getId();

        Task<List<Activity>> task = new Task<>() {
            private int followingCount = 0;

            @Override
            protected List<Activity> call() throws Exception {
                FollowDAO followDAO = new FollowDAO();
                followingCount = followDAO.getFollowingCount(userId);

                ActivityDAO activityDAO = new ActivityDAO();
                return activityDAO.findByFollowed(userId, newestFirst);
            }

            @Override
            protected void succeeded() {
                List<Activity> activities = getValue();
                activityPane.getChildren().clear();

                if (activities.isEmpty()) {
                    if (followingCount == 0) {
                        VBox placeholder = new VBox(12);
                        placeholder.setAlignment(Pos.CENTER);
                        placeholder.setPadding(new Insets(40, 16, 16, 16));

                        try {
                            Image img = new Image(
                                    ActivityLoader.class.getResourceAsStream("/images/user-regular.png"));
                            ImageView icon = new ImageView(img);
                            icon.setFitWidth(70);
                            icon.setFitHeight(86);
                            icon.setPreserveRatio(true);
                            icon.setOpacity(0.4);
                            placeholder.getChildren().add(icon);
                        } catch (Exception e) {
                            System.err.println("Impossibile caricare user-regular.png: " + e.getMessage());
                        }

                        Label msg = new Label("Trova persone da seguire per vedere la loro attività.");
                        msg.setTextFill(Color.GRAY);
                        msg.setFont(new Font(14));
                        msg.setWrapText(true);
                        msg.setTextAlignment(TextAlignment.CENTER);

                        placeholder.getChildren().add(msg);
                        activityPane.getChildren().add(placeholder);
                    } else {
                        Label msg = new Label("Nessuna attività recente.");
                        msg.setTextFill(Color.GRAY);
                        msg.setFont(new Font(14));
                        activityPane.getChildren().add(msg);
                    }
                } else {
                    for (Activity activity : activities) {
                        HBox row = new HBox(10);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setPadding(new Insets(6, 0, 6, 0));

                        Circle avatarBg = new Circle(16, Color.web("#3a3a3a"));
                        ImageView avatar = new ImageView();
                        avatar.setFitWidth(32);
                        avatar.setFitHeight(32);
                        avatar.setPreserveRatio(false);
                        Circle clip = new Circle(16, 16, 16);
                        avatar.setClip(clip);

                        Image img = com.ale.tuney.ImageLoader
                                .loadProfilePicture(activity.getUser().getProfileImageUrl());
                        if (img != null)
                            avatar.setImage(img);

                        javafx.scene.layout.StackPane avatarStack = new javafx.scene.layout.StackPane(avatarBg, avatar);
                        avatarStack.setPrefSize(32, 32);
                        avatarStack.setMinSize(32, 32);
                        avatarStack.setMaxSize(32, 32);

                        TextFlow flow = new TextFlow();
                        flow.setPrefWidth(180);
                        flow.setStyle("-fx-font-size: 13px;");
                        HBox.setHgrow(flow, Priority.ALWAYS);

                        Text userText = new Text("@" + activity.getUser().getUsername());
                        userText.setFill(Color.web("#ffb3d9"));
                        userText.getStyleClass().add("username-link");
                        userText.setStyle("-fx-font-weight: bold;");
                        userText.setOnMouseClicked(e -> {
                            if (row.getScene() != null) {
                                javafx.scene.layout.BorderPane mainRoot = (javafx.scene.layout.BorderPane) row
                                        .getScene().getRoot();
                                com.ale.tuney.controller.MainController mainCtrl = (com.ale.tuney.controller.MainController) mainRoot
                                        .getProperties().get("mainController");
                                if (mainCtrl != null) {
                                    mainCtrl.openUserProfile(activity.getUser());
                                }
                            }
                        });
                        flow.getChildren().add(userText);

                        if ("review".equals(activity.getType())) {
                            Text actionText = new Text(" ha recensito ");
                            actionText.setFill(Color.WHITE);
                            flow.getChildren().add(actionText);

                            Review review = activity.getReview();
                            String title = review != null && review.getTarget() != null ? review.getTarget().getTitle()
                                    : "Brano sconosciuto";
                            Text itemText = new Text("\"" + title + "\"");
                            itemText.setFill(Color.web(ITEM_COLOR));
                            itemText.getStyleClass().add("username-link");
                            itemText.setStyle("-fx-font-weight: bold;");

                            if (review != null) {
                                itemText.setOnMouseClicked(e -> {
                                    if (row.getScene() != null) {
                                        javafx.scene.layout.BorderPane mainRoot = (javafx.scene.layout.BorderPane) row
                                                .getScene().getRoot();
                                        try {
                                            FXMLLoader loader = new FXMLLoader(
                                                    com.ale.tuney.TuneyApplication.class
                                                            .getResource("review-detail-view.fxml"));
                                            javafx.scene.layout.BorderPane loadedRoot = loader.load();
                                            com.ale.tuney.controller.ReviewDetailController controller = loader
                                                    .getController();
                                            controller.setReview(review);
                                            controller.setPreviousCenterNode(mainRoot.getCenter());
                                            mainRoot.setCenter(loadedRoot.getCenter());
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                });
                            }
                            flow.getChildren().add(itemText);

                        } else if ("follow".equals(activity.getType())) {
                            Text actionText = new Text(" ha seguito un nuovo utente");
                            actionText.setFill(Color.WHITE);
                            flow.getChildren().add(actionText);

                        } else if ("like".equals(activity.getType())) {
                            Text actionText = new Text(" ha messo \"Mi piace\" alla recensione di ");
                            actionText.setFill(Color.WHITE);
                            flow.getChildren().add(actionText);

                            Review review = activity.getReview();
                            if (review != null) {
                                Text authorText = new Text("@" + review.getAuthor().getUsername());
                                authorText.setFill(Color.web("#ffb3d9"));
                                authorText.getStyleClass().add("username-link");
                                authorText.setStyle("-fx-font-weight: bold;");
                                authorText.setOnMouseClicked(e -> {
                                    if (row.getScene() != null) {
                                        javafx.scene.layout.BorderPane mainRoot = (javafx.scene.layout.BorderPane) row
                                                .getScene().getRoot();
                                        com.ale.tuney.controller.MainController mainCtrl = (com.ale.tuney.controller.MainController) mainRoot
                                                .getProperties().get("mainController");
                                        if (mainCtrl != null) {
                                            mainCtrl.openUserProfile(review.getAuthor());
                                        }
                                    }
                                });
                                flow.getChildren().add(authorText);

                                Text suText = new Text(" su ");
                                suText.setFill(Color.WHITE);
                                flow.getChildren().add(suText);

                                String title = review.getTarget() != null ? review.getTarget().getTitle()
                                        : "Brano sconosciuto";
                                Text itemText = new Text("\"" + title + "\"");
                                itemText.setFill(Color.web(ITEM_COLOR));
                                itemText.getStyleClass().add("username-link");
                                itemText.setStyle("-fx-font-weight: bold;");
                                itemText.setOnMouseClicked(e -> {
                                    if (row.getScene() != null) {
                                        javafx.scene.layout.BorderPane mainRoot = (javafx.scene.layout.BorderPane) row
                                                .getScene().getRoot();
                                        try {
                                            FXMLLoader loader = new FXMLLoader(
                                                    com.ale.tuney.TuneyApplication.class
                                                            .getResource("review-detail-view.fxml"));
                                            javafx.scene.layout.BorderPane loadedRoot = loader.load();
                                            com.ale.tuney.controller.ReviewDetailController controller = loader
                                                    .getController();
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
                                Text genericText = new Text(" una recensione");
                                genericText.setFill(Color.WHITE);
                                flow.getChildren().add(genericText);
                            }

                        } else if ("favorite".equals(activity.getType())) {
                            Text actionText = new Text(" ha aggiunto ");
                            actionText.setFill(Color.WHITE);
                            flow.getChildren().add(actionText);

                            com.ale.tuney.model.Item favItem = activity.getItem();
                            String favTitle = favItem != null ? favItem.getTitle() : "un brano";
                            Text itemText = new Text("\"" + favTitle + "\"");
                            itemText.setFill(Color.web(ITEM_COLOR));
                            itemText.setStyle("-fx-font-weight: bold;");
                            flow.getChildren().add(itemText);

                            Text suffixText = new Text(" ai suoi Preferiti");
                            suffixText.setFill(Color.WHITE);
                            flow.getChildren().add(suffixText);

                        } else if ("comment".equals(activity.getType())) {
                            Text actionText = new Text(" ha commentato la recensione di ");
                            actionText.setFill(Color.WHITE);
                            flow.getChildren().add(actionText);

                            Review review = activity.getReview();
                            if (review != null) {
                                Text authorText = new Text("@" + review.getAuthor().getUsername());
                                authorText.setFill(Color.web("#ffb3d9"));
                                authorText.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
                                authorText.setOnMouseClicked(e -> {
                                    if (row.getScene() != null) {
                                        javafx.scene.layout.BorderPane mainRoot = (javafx.scene.layout.BorderPane) row
                                                .getScene().getRoot();
                                        com.ale.tuney.controller.MainController mainCtrl = (com.ale.tuney.controller.MainController) mainRoot
                                                .getProperties().get("mainController");
                                        if (mainCtrl != null) {
                                            mainCtrl.openUserProfile(review.getAuthor());
                                        }
                                    }
                                });
                                flow.getChildren().add(authorText);

                                Text suText = new Text(" su ");
                                suText.setFill(Color.WHITE);
                                flow.getChildren().add(suText);

                                String title = review.getTarget() != null ? review.getTarget().getTitle()
                                        : "Brano sconosciuto";
                                Text itemText = new Text("\"" + title + "\"");
                                itemText.setFill(Color.web(ITEM_COLOR));
                                itemText.getStyleClass().add("username-link");
                                itemText.setStyle("-fx-font-weight: bold;");
                                itemText.setOnMouseClicked(e -> {
                                    if (row.getScene() != null) {
                                        javafx.scene.layout.BorderPane mainRoot = (javafx.scene.layout.BorderPane) row
                                                .getScene().getRoot();
                                        try {
                                            FXMLLoader loader = new FXMLLoader(
                                                    com.ale.tuney.TuneyApplication.class
                                                            .getResource("review-detail-view.fxml"));
                                            javafx.scene.layout.BorderPane loadedRoot = loader.load();
                                            com.ale.tuney.controller.ReviewDetailController controller = loader
                                                    .getController();
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
                                Text genericText = new Text(" una recensione");
                                genericText.setFill(Color.WHITE);
                                flow.getChildren().add(genericText);
                            }

                        } else {
                            Text actionText = new Text(" ha fatto qualcosa");
                            actionText.setFill(Color.WHITE);
                            flow.getChildren().add(actionText);
                        }

                        Label timeLabel = new Label(timeAgo(activity.getCreatedAt()));
                        timeLabel.setTextFill(Color.GRAY);
                        timeLabel.setFont(new Font(12));
                        timeLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

                        row.getChildren().addAll(avatarStack, flow, timeLabel);
                        activityPane.getChildren().add(row);
                    }
                }
            }

            @Override
            protected void failed() {
                System.err.println("Errore in ActivityLoader: " + getException().getMessage());
                getException().printStackTrace();
                activityPane.getChildren().clear();
                Label msg = new Label("Errore nel caricamento delle attività");
                msg.setTextFill(Color.RED);
                msg.setFont(new Font(14));
                activityPane.getChildren().add(msg);
            }
        };

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static String timeAgo(String createdAt) {
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
}