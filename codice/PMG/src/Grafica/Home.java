package Grafica;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Home extends Application {

    @Override
    public void start(Stage stage) {
    	// Titolo principale
        Label titolo = new Label("Benvenuto in ParkingM&G");
        titolo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        // Pulsanti
        Button utente = new Button("Sono un utente");
        Button operatore = new Button("Sono un operatore");

        utente.setPrefWidth(200);
        operatore.setPrefWidth(200);

        VBox root = new VBox(20, titolo, utente, operatore);
        root.setStyle("-fx-padding: 30; -fx-alignment: center;");

        Scene hScene = new Scene(root, 300, 250);
        
        // Scena utente
        Label uLabel = new Label("Pagina Utente");
        uLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button back = new Button("Torna Indietro");
        back.setOnAction(e -> stage.setScene(hScene));

        VBox uRoot = new VBox(20, uLabel, back);
        uRoot.setStyle("-fx-padding: 30; -fx-alignment: center;");

        Scene uScene = new Scene(uRoot, 350, 250);

        stage.setTitle("ParkingM&G");
        
        // Eventi bottoni
        utente.setOnAction(e -> stage.setScene(uScene));
        operatore.setOnAction(e -> System.out.println("Hai scelto: Operatore"));
        
        stage.setScene(hScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
