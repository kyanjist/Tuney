package com.ale.tuney.controller;

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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    @FXML private VBox step1;
    @FXML private VBox step2;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel1;

    @FXML private TextField usernameField;
    @FXML private TextField nameField;
    @FXML private Label errorLabel2;

    private String tempEmail;
    private String tempPassword;
    private double xOffset = 0;
    private double yOffset = 0;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void onNextClick() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel1.setText("Compila tutti i campi.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel1.setText("Email non valida.");
            return;
        }

        if (!password.matches("^(?=.*[A-Z])(?=.*\\d).{6,}$")) {
            errorLabel1.setText("La password deve contenere almeno 6 caratteri, una maiuscola e una cifra.");
            return;
        }

        try {
            if (userDAO.emailExists(email)) {
                errorLabel1.setText("Email già in uso.");
                return;
            }
        } catch (SQLException e) {
            errorLabel1.setText("Errore di connessione al database.");
            return;
        }

        tempEmail = email;
        tempPassword = password;

        step1.setVisible(false);
        step1.setManaged(false);
        step2.setVisible(true);
        step2.setManaged(true);
    }

    @FXML
    private void onRegisterClick() {
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();

        if (username.isEmpty() || name.isEmpty()) {
            errorLabel2.setText("Compila tutti i campi.");
            return;
        }

        try {
            if (userDAO.usernameExists(username)) {
                errorLabel2.setText("Username già in uso.");
                return;
            }
        } catch (SQLException e) {
            errorLabel2.setText("Errore di connessione al database.");
            return;
        }

        String hashedPassword = BCrypt.hashpw(tempPassword, BCrypt.gensalt(12));
        User newUser = new User(0, tempEmail, hashedPassword, username, name, null, null);

        try {
            userDAO.save(newUser);
        } catch (SQLException e) {
            errorLabel2.setText("Errore durante la registrazione.");
            return;
        }

        goToWelcome();
    }

    @FXML
    private void onBackClick() {
        goToWelcome();
    }

    private void goToWelcome() {
        try {
            FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("tuney-view.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
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
