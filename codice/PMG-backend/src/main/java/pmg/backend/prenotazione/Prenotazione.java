package pmg.backend.prenotazione;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import pmg.backend.posto.PostoResponse;

/**
 * Rappresenta un documento della collezione MongoDB delle prenotazioni.
 *
 * Contiene il ciclo di vita della prenotazione e uno snapshot del posto assegnato,
 * in modo da preservarne i dati anche dopo successive modifiche alla mappa del parcheggio.
 */
@Document(collection = "prenotazioni")
public class Prenotazione {

    /**
     * Identificativo univoco del documento.
     */
    @Id
    private String id;
    /**
     * Identificativo dell'utente associato.
     */
    private String utenteId;
    /**
     * Identificativo del parcheggio associato.
     */
    private String parcheggioId;
    /**
     * Data e ora di creazione.
     */
    private LocalDateTime dataCreazione;
    /**
     * Codice QR associato.
     */
    private String codiceQr;
    /**
     * Stato operativo corrente.
     */
    private StatoPrenotazione stato = StatoPrenotazione.attiva;
    /**
     * Data e ora di ingresso.
     */
    private LocalDateTime dataIngresso;
    /**
     * Data e ora di uscita.
     */
    private LocalDateTime dataUscita;
    /**
     * Importo registrato come pagato.
     */
    private Double importoPagato;
    /**
     * Data e ora del pagamento.
     */
    private LocalDateTime dataPagamento;
    /**
     * Snapshot del posto assegnato alla prenotazione.
     */
    private PostoResponse posto;
    /**
     * Data e ora entro cui deve essere validato l'ingresso.
     */
    private LocalDateTime scadenzaArrivo;

    /**
     * Costruttore vuoto richiesto dal framework di persistenza.
     */
    public Prenotazione() {
    }

    /**
     * Crea una nuova istanza di Prenotazione con i dati indicati.
     *
     * @param utenteId identificativo dell'utente
     * @param parcheggioId identificativo del parcheggio
     * @param dataCreazione data di creazione
     * @param codiceQr codice QR della prenotazione
     */
    public Prenotazione(
            String utenteId,
            String parcheggioId,
            LocalDateTime dataCreazione,
            String codiceQr) {
        this.utenteId = utenteId;
        this.parcheggioId = parcheggioId;
        this.dataCreazione = dataCreazione;
        this.codiceQr = codiceQr;
        this.stato = StatoPrenotazione.attiva;
    }

    /**
     * Restituisce identificativo.
     * @return identificativo
     */
    public String getId() {
        return id;
    }

    /**
     * Imposta identificativo.
     *
     * @param id identificativo della risorsa
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Restituisce identificativo dell'utente.
     * @return identificativo dell'utente
     */
    public String getUtenteId() {
        return utenteId;
    }

    /**
     * Imposta identificativo dell'utente.
     *
     * @param utenteId identificativo dell'utente
     */
    public void setUtenteId(String utenteId) {
        this.utenteId = utenteId;
    }

    /**
     * Restituisce identificativo del parcheggio.
     * @return identificativo del parcheggio
     */
    public String getParcheggioId() {
        return parcheggioId;
    }

    /**
     * Imposta identificativo del parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     */
    public void setParcheggioId(String parcheggioId) {
        this.parcheggioId = parcheggioId;
    }

    /**
     * Restituisce data di creazione.
     * @return data di creazione
     */
    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    /**
     * Imposta data di creazione.
     *
     * @param dataCreazione data di creazione
     */
    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    /**
     * Restituisce codice QR.
     * @return codice QR
     */
    public String getCodiceQr() {
        return codiceQr;
    }

    /**
     * Imposta codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     */
    public void setCodiceQr(String codiceQr) {
        this.codiceQr = codiceQr;
    }

    /**
     * Restituisce stato operativo.
     * @return stato operativo
     */
    public StatoPrenotazione getStato() {
        return stato;
    }

    /**
     * Imposta stato operativo.
     *
     * @param stato nuovo stato operativo
     */
    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
    }

    /**
     * Restituisce data di ingresso.
     * @return data di ingresso
     */
    public LocalDateTime getDataIngresso() {
        return dataIngresso;
    }

    /**
     * Imposta data di ingresso.
     *
     * @param dataIngresso data di ingresso
     */
    public void setDataIngresso(LocalDateTime dataIngresso) {
        this.dataIngresso = dataIngresso;
    }

    /**
     * Restituisce data di uscita.
     * @return data di uscita
     */
    public LocalDateTime getDataUscita() {
        return dataUscita;
    }

    /**
     * Imposta data di uscita.
     *
     * @param dataUscita data di uscita
     */
    public void setDataUscita(LocalDateTime dataUscita) {
        this.dataUscita = dataUscita;
    }

    /**
     * Restituisce importo pagato.
     * @return importo pagato
     */
    public Double getImportoPagato() {
        return importoPagato;
    }

    /**
     * Imposta importo pagato.
     *
     * @param importoPagato importo pagato
     */
    public void setImportoPagato(Double importoPagato) {
        this.importoPagato = importoPagato;
    }

    /**
     * Restituisce data del pagamento.
     * @return data del pagamento
     */
    public LocalDateTime getDataPagamento() {
        return dataPagamento;
    }

    /**
     * Imposta data del pagamento.
     *
     * @param dataPagamento data del pagamento
     */
    public void setDataPagamento(LocalDateTime dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    /**
     * Restituisce posto assegnato.
     * @return posto assegnato
     */
    public PostoResponse getPosto() {
        return posto;
    }

    /**
     * Imposta posto assegnato.
     *
     * @param posto posto da convertire o aggiornare
     */
    public void setPosto(PostoResponse posto) {
        this.posto = posto;
    }

    /**
     * Restituisce scadenza di arrivo.
     * @return scadenza di arrivo
     */
    public LocalDateTime getScadenzaArrivo() {
        return scadenzaArrivo;
    }

    /**
     * Imposta scadenza di arrivo.
     *
     * @param scadenzaArrivo scadenza di arrivo
     */
    public void setScadenzaArrivo(LocalDateTime scadenzaArrivo) {
        this.scadenzaArrivo = scadenzaArrivo;
    }
}
