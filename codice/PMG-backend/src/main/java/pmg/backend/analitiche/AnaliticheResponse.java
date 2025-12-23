package pmg.backend.analitiche;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pmg.backend.parcheggio.Parcheggio;

//import pmg.backend.parcheggio.Parcheggio;

public class AnaliticheResponse {

    private String id;
    private String parcheggioId;
    private String nomeParcheggio;
    private String operatoreId;

    private List<String> eventi;
    private List<String> allarmi;

    @Autowired
    private AnaliticheRepository analiticheRepository;

    public AnaliticheResponse() {}

    // Costruisce la response a partire dal parcheggio e operatore
    public AnaliticheResponse(Parcheggio parcheggio, String operatoreId) {
        this.parcheggioId = parcheggio.getId();
        this.nomeParcheggio = parcheggio.getNome();
        this.operatoreId = operatoreId;
        caricaAnalitiche();
    }

    private void caricaAnalitiche() {

        this.eventi = analiticheRepository
                .findByParcheggioIdAndOperatoreIdAndTipo(
                        parcheggioId, operatoreId, "EVENTO")
                .stream()
                .map(Analitiche::getDescrizione)
                .toList();

        this.allarmi = analiticheRepository
                .findByParcheggioIdAndOperatoreIdAndTipo(
                        parcheggioId, operatoreId, "ALLARME")
                .stream()
                .map(Analitiche::getDescrizione)
                .toList();
    }

    public String getId() {
        return id;
    }

    public String getParcheggioId() {
        return parcheggioId;
    }

    public String getNomeParcheggio() {
        return nomeParcheggio;
    }

    public String getOperatoreId() {
        return operatoreId;
    }

    public List<String> getEventi() {
        return eventi;
    }

    public List<String> getAllarmi() {
        return allarmi;
    }
}
