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
        Label label = new Label("Benvenuto in JavaFX!");
        Button button = new Button("Cliccami");

        button.setOnAction(e -> label.setText("Hai cliccato il pulsante!"));

        VBox root = new VBox(15, label, button);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene scene = new Scene(root, 300, 200);

        stage.setTitle("JavaFX Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
