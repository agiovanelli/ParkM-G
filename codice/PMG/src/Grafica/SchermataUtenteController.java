package Grafica;

import java.io.IOException;

import Utente.Utente;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class SchermataUtenteController {
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Preferenze.fxml"));
            Parent root = loader.load();
            
            PreferenzeController pc = loader.getController();
            
            pc.setUtente(u);

            Stage dialog = new Stage();
            dialog.initOwner(Main.getPrimaryStage());
            dialog.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            // stesso CSS della home
            scene.getStylesheets().add(getClass().getResource("home.css").toExternalForm());

            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Home.fxml"));
            
            u.logout();
            
            Stage stage = Main.getPrimaryStage();
            stage.setScene(new Scene(root));
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
}
