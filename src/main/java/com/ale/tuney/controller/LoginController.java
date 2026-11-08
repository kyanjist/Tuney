package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.TuneyApplication;
import com.ale.tuney.ThemeManager;
import com.ale.tuney.dao.UserDAO;
import com.ale.tuney.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField identifierField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserDAO userDAO = new UserDAO();
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void onLoginClick() {
        String identifier = identifierField.getText().trim();
        String password = passwordField.getText().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Compila tutti i campi.");
            return;
        }

        try {
            User user = userDAO.findByEmail(identifier);
            if (user == null) {
                user = userDAO.findByUsername(identifier);
            }

            if (user == null) {
                errorLabel.setText("Utente non trovato.");
                return;
            }

            if (!BCrypt.checkpw(password, user.getPassword())) {
                errorLabel.setText("Password errata.");
                return;
            }

            SessionManager.setCurrentUser(user);
            goToMain();

        } catch (SQLException e) {
            errorLabel.setText("Errore di connessione al database.");
        }
    }

    @FXML
    private void onBackClick() {
        goToWelcome();
    }

    private void goToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("home-view.fxml"));
            Stage stage = (Stage) identifierField.getScene().getWindow();
            javafx.scene.Parent root = loader.load();
            com.ale.tuney.ThemeManager.applyTheme(root);
            Scene scene = new Scene(root, 1280, 800);
            stage.setScene(scene);
            stage.setWidth(1280);
            stage.setHeight(800);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void goToWelcome() {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("tuney-view.fxml"));
            Stage stage = (Stage) identifierField.getScene().getWindow();
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
