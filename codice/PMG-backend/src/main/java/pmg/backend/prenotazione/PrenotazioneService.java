package pmg.backend.prenotazione;

import java.time.Clock;
import java.util.List;

/**
 * Servizio per la gestione delle prenotazioni.
 *
 * Definisce le operazioni principali per la gestione
 * del ciclo di vita di una prenotazione, dalla creazione
 * alla chiusura e pagamento.
 */
public interface PrenotazioneService {

    /**
     * Recupera lo storico delle prenotazioni di un utente.
     *
     * @param utenteId identificativo dell'utente
     * @return lista delle prenotazioni dell'utente
     */
    List<PrenotazioneResponse> getStoricoUtente(String utenteId);

    /**
     * Recupera le prenotazioni associate a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista delle prenotazioni
     */
    List<PrenotazioneResponse> getByParcheggio(String parcheggioId);

    /**
     * Valida l'ingresso tramite codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse validaIngresso(String codiceQr);

    /**
     * Annulla una prenotazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param utenteId identificativo dell'utente
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse annullaPrenotazione(String prenotazioneId, String utenteId);

    /**
     * Calcola l'importo da pagare per una prenotazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @return importo calcolato
     */
    double calcolaImporto(String prenotazioneId);

    /**
     * Effettua il pagamento di una prenotazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param importo importo da pagare
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse pagaPrenotazione(String prenotazioneId, double importo);

    /**
     * Valida l'uscita tramite codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse validaUscita(String codiceQr);

    /**
     * Recupera una prenotazione tramite codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione trovata
     */
    PrenotazioneResponse getPrenotazioneByQr(String codiceQr);

    /**
     * Conferma che l'utente ha effettivamente parcheggiato.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @return prenotazione aggiornata
     */
    PrenotazioneResponse confermaParcheggio(String prenotazioneId);

    /**
     * Imposta un clock personalizzato (utile per test).
     *
     * @param fixedClock clock da utilizzare
     */
    void setClock(Clock fixedClock);
}