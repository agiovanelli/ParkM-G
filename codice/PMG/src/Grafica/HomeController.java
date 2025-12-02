package Grafica;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HomeController {

    @FXML
    private void onUtente() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("SchermataUtente.fxml"));
            Stage stage = Main.getPrimaryStage();
            stage.setScene(new Scene(root, 350, 250));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onOperatore() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("SchermataOperatore.fxml"));
            Stage stage = Main.getPrimaryStage();
            stage.setScene(new Scene(root, 350, 250));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
