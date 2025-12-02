package Grafica;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SchermataUtente {
    private Scene scene;

    public SchermataUtente(Stage stage) {

        Label label = new Label("Pagina Utente");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button back = new Button("Torna indietro");
        back.setOnAction(e -> {
            Home home = new Home(stage);
            stage.setScene(home.getScene());
        });

        VBox layout = new VBox(20, label, back);
        layout.setStyle("-fx-padding: 30; -fx-alignment: center;");

        scene = new Scene(layout, 350, 250);
    }

    public Scene getScene() {
        return scene;
    }
}
