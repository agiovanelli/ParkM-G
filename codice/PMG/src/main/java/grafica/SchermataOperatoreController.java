package grafica;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class SchermataOperatoreController {
	
private static final Logger LOGGER = LoggerFactory.getLogger(SchermataOperatoreController.class); 
	
private Stage stage;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
	@FXML
    private void onBack() {
        try {
        	
        	if (this.stage == null) {
                LOGGER.info("Errore: Lo Stage non è stato iniettato.");
                return;
            }
        	Parent root = FXMLLoader.load(getClass().getResource("Home.fxml"));
        	this.stage.setScene(new Scene(root));
        	
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            this.stage.setX(bounds.getMinX());
            this.stage.setY(bounds.getMinY());
            this.stage.setWidth(bounds.getWidth());
            this.stage.setHeight(bounds.getHeight());
            this.stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

