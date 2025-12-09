package grafica;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

   

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Home.fxml"));
        Parent root = loader.load();

        HomeController controller = loader.getController();
        controller.setStage(stage); // Passi lo stage al controller

        Scene scene = new Scene(root, 300, 250);
        stage.setTitle("ParkingM&G");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    

    public static void main(String[] args) {
        launch(args);
    }
}
