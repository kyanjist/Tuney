package com.ale.tuney.controller;

import com.ale.tuney.SessionManager;
import com.ale.tuney.ThemeManager;
import com.ale.tuney.TuneyApplication;
import com.ale.tuney.dao.UserDAO;
import com.ale.tuney.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label pwdErrorLabel;

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

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
    }

    @FXML
    private void changePassword() {
        User user = SessionManager.getCurrentUser();
        if (user == null)
            return;

        String current = currentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        pwdErrorLabel.setVisible(true);
        pwdErrorLabel.setManaged(true);

        if (current.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            pwdErrorLabel.setTextFill(Color.web("#ff5555"));
            pwdErrorLabel.setText("Tutti i campi sono obbligatori.");
            return;
        }

        if (!newPwd.equals(confirm)) {
            pwdErrorLabel.setTextFill(Color.web("#ff5555"));
            pwdErrorLabel.setText("La nuova password e la conferma non coincidono.");
            return;
        }

        if (!newPwd.matches("^(?=.*[A-Z])(?=.*\\d).{6,}$")) {
            pwdErrorLabel.setTextFill(Color.web("#ff5555"));
            pwdErrorLabel.setText("La nuova password deve contenere almeno 6 caratteri, una maiuscola e una cifra.");
            return;
        }

        if (!BCrypt.checkpw(current, user.getPassword())) {
            pwdErrorLabel.setTextFill(Color.web("#ff5555"));
            pwdErrorLabel.setText("La password corrente inserita non è corretta.");
            return;
        }

        try {
            String newHashed = BCrypt.hashpw(newPwd, BCrypt.gensalt(12));
            userDAO.updatePassword(user.getId(), newHashed);

            user.setPassword(newHashed);

            pwdErrorLabel.setTextFill(Color.web("#9b6fd4"));
            pwdErrorLabel.setText("Password aggiornata con successo!");

            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (SQLException e) {
            e.printStackTrace();
            pwdErrorLabel.setTextFill(Color.web("#ff5555"));
            pwdErrorLabel.setText("Errore del database durante l'aggiornamento.");
        }
    }

    @FXML
    private void deleteAccount() {
        User user = SessionManager.getCurrentUser();
        if (user == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Elimina Account");
        alert.setHeaderText("Sei sicuro di voler eliminare permanentemente l'account?");
        alert.setContentText(
                "Tutti i tuoi dati (recensioni, mi piace, commenti, elementi salvati) verranno cancellati per sempre. Questa operazione non può essere annullata.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Conferma Eliminazione");
            dialog.setHeaderText(
                    "Per confermare l'eliminazione definitiva, inserisci il testo di sicurezza richiesto.");
            dialog.setContentText("Digita la parola 'ELIMINA' in maiuscolo:");

            Optional<String> confirmResult = dialog.showAndWait();
            if (confirmResult.isPresent() && confirmResult.get().trim().equals("ELIMINA")) {
                try {
                    userDAO.deleteUser(user.getId());
                    SessionManager.logout();

                    FXMLLoader loader = new FXMLLoader(TuneyApplication.class.getResource("tuney-view.fxml"));
                    Stage stage = (Stage) scrollPane.getScene().getWindow();
                    Scene scene = new Scene(loader.load());
                    stage.setScene(scene);
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Errore");
                    errorAlert.setHeaderText("Impossibile eliminare l'account");
                    errorAlert.setContentText("Si è verificato un errore durante l'operazione nel database.");
                    errorAlert.showAndWait();
                }
            }
        }
    }
}
