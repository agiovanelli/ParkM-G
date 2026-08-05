package pmg.backend.prenotazione;

import java.time.Clock;
import java.util.List;

/**
 * Servizio applicativo per la gestione del ciclo di vita delle prenotazioni.
 */
public interface PrenotazioneService {

    /**
     * Recupera lo storico delle prenotazioni dell'utente.
     *
     * @param utenteId identificativo dell'utente
     * @return storico delle prenotazioni
     */
    List<PrenotazioneResponse> getStoricoUtente(String utenteId);

    /**
     * Recupera gli elementi associati al parcheggio indicato.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista dei posti del parcheggio
     */
    List<PrenotazioneResponse> getByParcheggio(String parcheggioId);

    /**
     * Valida l'ingresso tramite il codice QR della prenotazione.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse validaIngresso(String codiceQr);

    /**
     * Annulla una prenotazione e libera il posto associato.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param utenteId identificativo dell'utente
     * @return prenotazione annullata
     */
    PrenotazioneResponse annullaPrenotazione(
            String prenotazioneId,
            String utenteId);

    /**
     * Calcola l'importo dovuto in base a durata, preferenze, fascia oraria e occupazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @return importo calcolato
     */
    double calcolaImporto(String prenotazioneId);

    /**
     * Registra il pagamento e aggiorna lo stato della prenotazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param importo importo da registrare
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse pagaPrenotazione(
            String prenotazioneId,
            double importo);

    /**
     * Valida l'uscita e rende nuovamente libero il posto.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione conclusa
     */
    PrenotazioneResponse validaUscita(String codiceQr);

    /**
     * Recupera una prenotazione tramite il relativo codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione trovata
     */
    PrenotazioneResponse getPrenotazioneByQr(String codiceQr);

    /**
     * Conferma che il veicolo ha raggiunto e occupato il posto assegnato.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse confermaParcheggio(String prenotazioneId);

    /**
     * Imposta il clock da utilizzare nelle operazioni temporali, principalmente per i test.
     *
     * @param fixedClock clock da utilizzare
     */
    void setClock(Clock fixedClock);
}
