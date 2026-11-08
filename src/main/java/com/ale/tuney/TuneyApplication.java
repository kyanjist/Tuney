package com.ale.tuney;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class TuneyApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        boolean autoLoginSuccess = SessionManager.tryAutoLogin();
        String fxmlFile = autoLoginSuccess ? "home-view.fxml" : "tuney-view.fxml";

        FXMLLoader fxmlLoader = new FXMLLoader(TuneyApplication.class.getResource(fxmlFile));
        javafx.scene.Parent root = fxmlLoader.load();

        ThemeManager.applyTheme(root);

        Scene scene = new Scene(root, 1280, 800);
        scene.setFill(null);

        stage.setTitle("Tuney");

        // icona dell'app
        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icon/icon.png"))
        );

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.show();
    }
}
