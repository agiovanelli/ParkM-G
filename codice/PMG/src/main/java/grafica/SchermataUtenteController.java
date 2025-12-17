package grafica;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import utente.Utente;

public class SchermataUtenteController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SchermataUtenteController.class); 
private Stage stage;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
	
	private Utente u;

    @FXML
    private void initialize() {
        
    }
    
    // Mostra il popup "Preferenze" all'apertura della schermata
    public void setUtente(Utente u) {
        this.u = u;

        if(u.getPreferenze() == null) {
        	showPreferenzeDialog();
        }
    }

    private void showPreferenzeDialog() {
       
        if (this.stage == null) {
            LOGGER.error("Impossibile mostrare il dialog: Stage principale non iniettato.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Preferenze.fxml"));
            Parent root = loader.load();
            
            PreferenzeController pc = loader.getController();
            
            pc.setUtente(u);

            Stage dialog = new Stage();
            
            // 💡 CORREZIONE: Usa lo Stage iniettato (this.stage) come proprietario
            dialog.initOwner(this.stage); 
            
            dialog.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            // stesso CSS della home
            scene.getStylesheets().add(getClass().getResource("home.css").toExternalForm());

            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();
        } catch (IOException e) {
            LOGGER.error("Errore durante il caricamento del dialog Preferenze.", e);
            e.printStackTrace();
        }
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
