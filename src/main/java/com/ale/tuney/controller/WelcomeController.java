package com.ale.tuney.controller;

import com.ale.tuney.TuneyApplication;
import com.ale.tuney.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class WelcomeController {

    @FXML
    private void onLoginClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("login-view.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1280, 800));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegisterClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("register-view.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1280, 800));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeApp(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private double xOffset = 0;
    private double yOffset = 0;

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
}