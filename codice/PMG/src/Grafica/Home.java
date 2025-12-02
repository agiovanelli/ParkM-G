package Grafica;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Home {

    private Scene scene;

    public Home(Stage stage) {

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

        // ⬅️ QUI salviamo la scena
        scene = new Scene(root, 300, 250);

        // Eventi bottoni
        utente.setOnAction(e -> {
            SchermataUtente viewUtente = new SchermataUtente(stage);
            stage.setScene(viewUtente.getScene());
        });

        operatore.setOnAction(e -> {
            SchermataOperatore viewOperatore = new SchermataOperatore(stage);
            stage.setScene(viewOperatore.getScene());
        });
    }

    public Scene getScene() {
        return scene;
    }
}
