package Grafica;

import java.io.IOException;

import Database.QueryOperatori;
import Operatore.Operatore;
import Utente.Utente;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class HomeController {
	
	private Utente u;
	private Operatore o;

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
    @FXML private TextField userRegisterSurname;
    @FXML private TextField userRegisterEmail;
    @FXML private PasswordField userRegisterPassword;

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

    // Utenti
    @FXML
    private void handleUserLogin(ActionEvent event){
        String email = userLoginEmail.getText();
        String password = userLoginPassword.getText();

        // Controllo campi vuoti
        if (email.isEmpty() || password.isEmpty()) {
            showError("Inserisci email e password.");
            return;
        }

        // Controllo formato email
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Formato email non valido.");
            return;
        }
        
        // Se login ok -> vai alla schermata utente
        try {
        	u = new Utente(password, email);
        	
        	if(!u.loginDB()){
        		showError("Questo utente non esiste");
        		return;
        	}
        	
        	FXMLLoader loader = new FXMLLoader(getClass().getResource("SchermataUtente.fxml"));
        	Parent root = loader.load();
            
            SchermataUtenteController suc = loader.getController();
            
            suc.setUtente(u);
            
            Stage stage = Main.getPrimaryStage();
            
            stage.setScene(new Scene(root));
            stage.setResizable(true); 
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            stage.setMaximized(true); 
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
        	e2.printStackTrace();
        }        
    }

    @FXML
    private void handleUserRegister(ActionEvent event) {
        String nome = userRegisterName.getText();
        String cognome = userRegisterSurname.getText();
        String email = userRegisterEmail.getText();
        String pwd = userRegisterPassword.getText();

        if (nome.isEmpty() || cognome.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
            showError("Compila tutti i campi.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Email non valida.");
            return;
        }

        // Controllo password
        if (!passwordValida(pwd)) {
            showError("La password deve contenere almeno:\n- 6 caratteri\n- 1 lettera maiuscola\n- 1 numero\n- 1 carattere speciale");
            return;
        }

        try {
        	u = new Utente(nome, cognome, pwd, email);
        	
        	if(u.controlloCredenziali() != null){
        		showError("Le credenziali sono già in uso");
        		return;
        	}
        	
        	userLoginPane.setVisible(true);
            userLoginPane.setManaged(true);

            userRegisterPane.setVisible(false);
            userRegisterPane.setManaged(false);
            
            userModeGroup.selectToggle(btnUserLogin);
        }catch(Exception e) {
        	e.printStackTrace();
        }
    }

    @FXML
    private void handleUserForgotPassword(ActionEvent event) {
        // TODO: apri finestra/scene per recupero password
        System.out.println("Richiesto recupero password utente");
    }

    // Operatori
    @FXML
    private void handleOperatorLogin(ActionEvent event) {
        String nomeStruttura = operatorCode.getText();
        String username = operatorPassword.getText();

     // Controllo campi vuoti
        if (nomeStruttura.isEmpty() || username.isEmpty()) {
            showError("Inserisci il nome della struttura e l'username.");
            return;
        }

        // Se login ok -> vai alla schermata operatore
        try {
        	o = new Operatore(nomeStruttura, username);
        	QueryOperatori qo = new QueryOperatori();
        	
        	if(!qo.esisteOperatore(o)){
        		showError("Questo operatore non esiste");
        		return;
        	}
        	
            Parent root = FXMLLoader.load(getClass().getResource("SchermataOperatore.fxml"));
            Stage stage = Main.getPrimaryStage();
            stage.setScene(new Scene(root));
            stage.setResizable(true); 
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private boolean passwordValida(String pwd) {
        if (pwd == null) return false;

        // Lunghezza minima
        if (pwd.length() < 6) {
            return false;
        }

        // Almeno una maiuscola
        if (!pwd.matches(".*[A-Z].*")) {
            return false;
        }

        // Almeno un numero
        if (!pwd.matches(".*\\d.*")) {
            return false;
        }

        // Almeno un carattere speciale
        if (!pwd.matches(".*[!@#$%^&*()_+\\-={}|\\[\\]:\";'<>?,./].*")) {
            return false;
        }

        return true;
    }
    
    private void showError(String msg) {
        javafx.scene.control.Alert alert = 
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
