package Grafica;

import java.util.HashMap;
import java.util.Map;

import Utente.Utente;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class PreferenzeController {
	private Map<String, String> pref = new HashMap<>();
	private Utente u;

    // Età
    @FXML private RadioButton rbUnder30;
    @FXML private RadioButton rbOver60;
    private final ToggleGroup ageGroup = new ToggleGroup();

    // Piano
    @FXML private RadioButton rbPianoTerra;
    @FXML private RadioButton rbAltriPiani;
    private final ToggleGroup floorGroup = new ToggleGroup();

    // Distanza
    @FXML private Slider sliderDistanza;

    // Condizioni speciali
    @FXML private CheckBox cbDisabile;
    @FXML private CheckBox cbDonnaIncinta;

    // Occupazione
    @FXML private ChoiceBox<String> choiceOccupazione;

    @FXML
    private void initialize() {
        // Associo i RadioButton ai rispettivi gruppi
        rbUnder30.setToggleGroup(ageGroup);
        rbOver60.setToggleGroup(ageGroup);

        rbPianoTerra.setToggleGroup(floorGroup);
        rbAltriPiani.setToggleGroup(floorGroup);

        // Default: under 30, piano terra
        ageGroup.selectToggle(rbUnder30);
        floorGroup.selectToggle(rbPianoTerra);

        // Valori ChoiceBox
        choiceOccupazione.getItems().setAll(
                "Studente",
                "Lavoratore dipendente",
                "Libero professionista"
        );
        choiceOccupazione.getSelectionModel().selectFirst();

        // Default slider
        sliderDistanza.setValue(30);
    }

    @FXML
    private void onAnnulla(javafx.event.ActionEvent event) {
        close(event);
    }

    @FXML
    private void onSalva(javafx.event.ActionEvent event) {
        // Lettura valori (per debug o per salvataggio)
        String eta;
        if (ageGroup.getSelectedToggle() == rbUnder30) {
            eta = "under30";
        } else if (ageGroup.getSelectedToggle() == rbOver60) {
            eta = "over60";
        } else {
            eta = "nessuna";
        }

        String piano;
        if (floorGroup.getSelectedToggle() == rbPianoTerra) {
            piano = "piano_terra";
        } else if (floorGroup.getSelectedToggle() == rbAltriPiani) {
            piano = "altri_piani";
        } else {
            piano = "nessuna";
        }

        String distanza = String.format("%.2f", sliderDistanza.getValue());
        String disabile = cbDisabile.isSelected()? "Sì" : "No";
        String donnaIncinta = cbDonnaIncinta.isSelected()? "Sì" : "No";
        String occupazione = choiceOccupazione.getValue();
        
        pref.put("età", eta);
        pref.put("piano", piano);
        pref.put("distanza", distanza);
        pref.put("disabile", disabile);
        pref.put("donnaIncinta", donnaIncinta);
        pref.put("occupazione", occupazione);
        
        u.setSelezione(pref);

        close(event);
    }

    private void close(javafx.event.ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
    
    public void setUtente(Utente u) {
        this.u = u;
    }
}
