package Grafica;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class PreferenzeController {

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

        double distanza = sliderDistanza.getValue();
        boolean disabile = cbDisabile.isSelected();
        boolean donnaIncinta = cbDonnaIncinta.isSelected();
        String occupazione = choiceOccupazione.getValue();

        System.out.println("Preferenze salvate:");
        System.out.println("  Età: " + eta);
        System.out.println("  Piano: " + piano);
        System.out.println("  Distanza: " + distanza);
        System.out.println("  Disabile: " + disabile);
        System.out.println("  Donna incinta: " + donnaIncinta);
        System.out.println("  Occupazione: " + occupazione);

        close(event);
    }

    private void close(javafx.event.ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
