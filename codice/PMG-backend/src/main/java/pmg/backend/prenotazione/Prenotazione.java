package pmg.backend.prenotazione;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import pmg.backend.posto.PostoResponse;

import java.time.LocalDateTime;

@Document(collection = "prenotazioni")
public class Prenotazione {

    @Id
    private String id;
    private String utenteId;
    private String parcheggioId;
    private LocalDateTime dataCreazione;
    private String codiceQr;
    private StatoPrenotazione stato = StatoPrenotazione.attiva;
    private LocalDateTime dataIngresso;
    private LocalDateTime dataUscita;
    private Double importoPagato;
    private LocalDateTime dataPagamento;
    private PostoResponse posto;
    private LocalDateTime scadenzaArrivo;
    
    // Costruttore vuoto richiesto da Spring / Mongo
    public Prenotazione() {}

    // Costruttore "comodo" 
    public Prenotazione(String utenteId, String parcheggioId, LocalDateTime dataCreazione, String codiceQr) {
        this.utenteId = utenteId;
        this.parcheggioId = parcheggioId;
        this.dataCreazione = dataCreazione;
        this.codiceQr = codiceQr;
        this.stato = StatoPrenotazione.attiva;
        this.dataIngresso = null;
        this.dataUscita = null;
    }
    
    // Getter e Setter 
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUtenteId() {
        return utenteId;
    }

    public void setUtenteId(String utenteId) {
        this.utenteId = utenteId;
    }

    public String getParcheggioId() {
        return parcheggioId;
    }

    public void setParcheggioId(String parcheggioId) {
        this.parcheggioId = parcheggioId;
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public String getCodiceQr() {
        return codiceQr;
    }

    public void setCodiceQr(String codiceQr) {
        this.codiceQr = codiceQr;
    }

    public StatoPrenotazione getStato() {
        return stato;
    }

    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
    }

    public LocalDateTime getDataIngresso() {
        return dataIngresso;
    }

    public void setDataIngresso(LocalDateTime dataIngresso) {
        this.dataIngresso = dataIngresso;
    }

    public LocalDateTime getDataUscita() {
        return dataUscita;
    }

    public void setDataUscita(LocalDateTime dataUscita) {
        this.dataUscita = dataUscita;
    }
    public Double getImportoPagato() { return importoPagato; }
    public void setImportoPagato(Double importoPagato) { this.importoPagato = importoPagato; }
    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }
    public PostoResponse getPosto() { return posto; }
    public void setPosto(PostoResponse posto) { this.posto = posto; }
    
    public LocalDateTime getScadenzaArrivo() {
        return scadenzaArrivo;
    }

    public void setScadenzaArrivo(LocalDateTime scadenzaArrivo) {
        this.scadenzaArrivo = scadenzaArrivo;
    }
}

record PrenotazioneDate(LocalDateTime dataCreazione, LocalDateTime dataIngresso,
                        LocalDateTime dataUscita) {}
