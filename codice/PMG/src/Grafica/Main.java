package Grafica;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("ParkingM&G");
        Home home = new Home(stage); // schermata iniziale
        stage.setScene(home.getScene());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
