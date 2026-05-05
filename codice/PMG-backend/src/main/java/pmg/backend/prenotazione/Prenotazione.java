package pmg.backend.prenotazione;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import pmg.backend.posto.PostoResponse;

import java.time.LocalDateTime;

/**
 * Rappresenta un documento della collezione MongoDB delle prenotazioni.
 *
 * Contiene le informazioni principali associate a una prenotazione,
 * come utente, parcheggio, stato e dettagli temporali.
 */
@Document(collection = "prenotazioni")
public class Prenotazione {

    /** Identificativo univoco del documento. */
    @Id
    private String id;

    /** Identificativo dell'utente che ha effettuato la prenotazione. */
    private String utenteId;

    /** Identificativo del parcheggio associato. */
    private String parcheggioId;

    /** Data e ora di creazione della prenotazione. */
    private LocalDateTime dataCreazione;

    /** Codice QR associato alla prenotazione. */
    private String codiceQr;

    /** Stato corrente della prenotazione. */
    private StatoPrenotazione stato = StatoPrenotazione.attiva;

    /** Data e ora di ingresso nel parcheggio. */
    private LocalDateTime dataIngresso;

    /** Data e ora di uscita dal parcheggio. */
    private LocalDateTime dataUscita;

    /** Importo pagato per la prenotazione. */
    private Double importoPagato;

    /** Data e ora del pagamento. */
    private LocalDateTime dataPagamento;

    /** Informazioni del posto assegnato. */
    private PostoResponse posto;

    /** Scadenza entro cui l'utente deve arrivare. */
    private LocalDateTime scadenzaArrivo;
    
    /**
     * Costruttore vuoto richiesto da Spring Data MongoDB.
     */
    public Prenotazione() {}

    /**
     * Crea una nuova prenotazione con i dati principali.
     *
     * @param utenteId identificativo dell'utente
     * @param parcheggioId identificativo del parcheggio
     * @param dataCreazione data e ora di creazione
     * @param codiceQr codice QR associato
     */
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

    /**
     * Restituisce l'identificativo del documento.
     *
     * @return identificativo del documento
     */
    public String getId() {
        return id;
    }

    /**
     * Imposta l'identificativo del documento.
     *
     * @param id nuovo identificativo del documento
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Restituisce l'identificativo dell'utente.
     *
     * @return identificativo dell'utente
     */
    public String getUtenteId() {
        return utenteId;
    }

    /**
     * Imposta l'identificativo dell'utente.
     *
     * @param utenteId nuovo identificativo dell'utente
     */
    public void setUtenteId(String utenteId) {
        this.utenteId = utenteId;
    }

    /**
     * Restituisce l'identificativo del parcheggio.
     *
     * @return identificativo del parcheggio
     */
    public String getParcheggioId() {
        return parcheggioId;
    }

    /**
     * Imposta l'identificativo del parcheggio.
     *
     * @param parcheggioId nuovo identificativo del parcheggio
     */
    public void setParcheggioId(String parcheggioId) {
        this.parcheggioId = parcheggioId;
    }

    /**
     * Restituisce la data di creazione.
     *
     * @return data di creazione
     */
    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    /**
     * Imposta la data di creazione.
     *
     * @param dataCreazione nuova data di creazione
     */
    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    /**
     * Restituisce il codice QR.
     *
     * @return codice QR
     */
    public String getCodiceQr() {
        return codiceQr;
    }

    /**
     * Imposta il codice QR.
     *
     * @param codiceQr nuovo codice QR
     */
    public void setCodiceQr(String codiceQr) {
        this.codiceQr = codiceQr;
    }

    /**
     * Restituisce lo stato della prenotazione.
     *
     * @return stato della prenotazione
     */
    public StatoPrenotazione getStato() {
        return stato;
    }

    /**
     * Imposta lo stato della prenotazione.
     *
     * @param stato nuovo stato
     */
    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
    }

    /**
     * Restituisce la data di ingresso.
     *
     * @return data di ingresso
     */
    public LocalDateTime getDataIngresso() {
        return dataIngresso;
    }

    /**
     * Imposta la data di ingresso.
     *
     * @param dataIngresso nuova data di ingresso
     */
    public void setDataIngresso(LocalDateTime dataIngresso) {
        this.dataIngresso = dataIngresso;
    }

    /**
     * Restituisce la data di uscita.
     *
     * @return data di uscita
     */
    public LocalDateTime getDataUscita() {
        return dataUscita;
    }

    /**
     * Imposta la data di uscita.
     *
     * @param dataUscita nuova data di uscita
     */
    public void setDataUscita(LocalDateTime dataUscita) {
        this.dataUscita = dataUscita;
    }

    /**
     * Restituisce l'importo pagato.
     *
     * @return importo pagato
     */
    public Double getImportoPagato() { return importoPagato; }

    /**
     * Imposta l'importo pagato.
     *
     * @param importoPagato nuovo importo
     */
    public void setImportoPagato(Double importoPagato) { this.importoPagato = importoPagato; }

    /**
     * Restituisce la data del pagamento.
     *
     * @return data del pagamento
     */
    public LocalDateTime getDataPagamento() { return dataPagamento; }

    /**
     * Imposta la data del pagamento.
     *
     * @param dataPagamento nuova data del pagamento
     */
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }

    /**
     * Restituisce il posto assegnato.
     *
     * @return posto assegnato
     */
    public PostoResponse getPosto() { return posto; }

    /**
     * Imposta il posto assegnato.
     *
     * @param posto nuovo posto
     */
    public void setPosto(PostoResponse posto) { this.posto = posto; }
    
    /**
     * Restituisce la scadenza di arrivo.
     *
     * @return scadenza di arrivo
     */
    public LocalDateTime getScadenzaArrivo() {
        return scadenzaArrivo;
    }

    /**
     * Imposta la scadenza di arrivo.
     *
     * @param scadenzaArrivo nuova scadenza
     */
    public void setScadenzaArrivo(LocalDateTime scadenzaArrivo) {
        this.scadenzaArrivo = scadenzaArrivo;
    }
}

/**
 * Rappresenta un contenitore per le principali date di una prenotazione.
 *
 * Permette di raggruppare le informazioni temporali in un'unica struttura.
 *
 * @param dataCreazione data di creazione
 * @param dataIngresso data di ingresso
 * @param dataUscita data di uscita
 */
record PrenotazioneDate(LocalDateTime dataCreazione, LocalDateTime dataIngresso,
                        LocalDateTime dataUscita) {}