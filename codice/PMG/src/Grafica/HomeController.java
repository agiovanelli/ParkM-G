package Grafica;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HomeController {

    // --- Toggle Accedi / Registrati ---
    @FXML private ToggleGroup userModeGroup;
    @FXML private ToggleButton btnUserLogin;
    @FXML private ToggleButton btnUserRegister;

    // Contenitori dei due form
    @FXML private VBox userLoginPane;
    @FXML private VBox userRegisterPane;

    // --- Campi login utente ---
    @FXML private TextField userLoginEmail;
    @FXML private PasswordField userLoginPassword;

    // --- Campi registrazione utente ---
    @FXML private TextField userRegisterName;
    @FXML private TextField userRegisterEmail;
    @FXML private PasswordField userRegisterPassword;
    @FXML private PasswordField userRegisterConfirmPassword;

    // --- Campi login operatore ---
    @FXML private TextField operatorCode;
    @FXML private PasswordField operatorPassword;

    // (eventuale link "Password dimenticata?")
    @FXML private Hyperlink forgotPasswordLink;

    @FXML
    private void initialize() {
        // Listener per mostrare/nascondere i due pannelli quando cambio toggle
        userModeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean loginSelected = newT == btnUserLogin;

            userLoginPane.setVisible(loginSelected);
            userLoginPane.setManaged(loginSelected);

            userRegisterPane.setVisible(!loginSelected);
            userRegisterPane.setManaged(!loginSelected);
        });
    }

    // ------------------------------------------------
    //  CLIENTI
    // ------------------------------------------------

    @FXML
    private void handleUserLogin(ActionEvent event) {
        String email = userLoginEmail.getText();
        String password = userLoginPassword.getText();

        // TODO: qui metti la tua logica di verifica credenziali (DB, ecc.)
        System.out.println("Login utente: " + email + " / " + password);

        // Se login ok -> vai alla schermata utente
        try {
            Parent root = FXMLLoader.load(getClass().getResource("SchermataUtente.fxml"));
            Stage stage = Main.getPrimaryStage();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUserRegister(ActionEvent event) {
        String nome = userRegisterName.getText();
        String email = userRegisterEmail.getText();
        String pwd = userRegisterPassword.getText();
        String pwd2 = userRegisterConfirmPassword.getText();

        // TODO: controlli base (password uguali, email non vuota, ecc.)
        System.out.println("Registrazione utente: " + nome + " / " + email);

        // TODO: salva sul DB, poi eventualmente:
        // - mostra un messaggio di successo
        // - passa automaticamente al tab login
    }

    @FXML
    private void handleUserForgotPassword(ActionEvent event) {
        // TODO: apri finestra/scene per recupero password
        System.out.println("Richiesto recupero password utente");
    }

    // ------------------------------------------------
    //  OPERATORI
    // ------------------------------------------------

    @FXML
    private void handleOperatorLogin(ActionEvent event) {
        String code = operatorCode.getText();
        String pwd = operatorPassword.getText();

        // TODO: logica di verifica codice operatore + password
        System.out.println("Login operatore: " + code);

        // Se login ok -> vai alla schermata operatore
        try {
            Parent root = FXMLLoader.load(getClass().getResource("SchermataOperatore.fxml"));
            Stage stage = Main.getPrimaryStage();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
