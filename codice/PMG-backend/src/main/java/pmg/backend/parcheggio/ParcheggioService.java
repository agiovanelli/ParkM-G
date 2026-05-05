package pmg.backend.parcheggio;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

/**
 * Servizio per la gestione dei parcheggi.
 *
 * Definisce le operazioni principali per la ricerca,
 * prenotazione e gestione dei parcheggi e dei posti.
 */
@Service
public interface ParcheggioService {

    /**
     * Cerca parcheggi in base all'area geografica.
     *
     * @param area area da cercare
     * @return lista dei parcheggi trovati
     */
    List<ParcheggioResponse> cercaPerArea(String area);

    /**
     * Effettua una prenotazione di un posto auto.
     *
     * @param req dati della richiesta di prenotazione
     * @return prenotazione creata
     */
    PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req);

    /**
     * Cerca parcheggi vicini a una posizione geografica.
     *
     * @param lat latitudine
     * @param lng longitudine
     * @param radius raggio di ricerca in metri
     * @return lista dei parcheggi trovati
     */
    List<ParcheggioResponse> cercaVicini(double lat, double lng, double radius);

    /**
     * Imposta lo stato di emergenza di un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param stato stato di emergenza (true/false)
     * @param motivo motivo dell'emergenza
     */
    void impostaStatoEmergenza(String parcheggioId, boolean stato, String motivo);

    /**
     * Assegna il posto ottimale in base alle preferenze utente.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param preferenze preferenze utente
     * @return posto selezionato o null se non disponibile
     */
    Posto assegnaPostoOttimale(String parcheggioId, Map<String, String> preferenze);

    /**
     * Recupera un parcheggio tramite il suo identificativo.
     *
     * @param id identificativo del parcheggio
     * @return parcheggio corrispondente all'identificativo indicato
     */
    ParcheggioResponse getById(String id);

    /**
     * Recupera i posti di un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano opzionale
     * @return lista dei posti
     */
    List<PostoResponse> getPosti(String parcheggioId, Integer piano);

    /**
     * Sincronizza le statistiche dei posti di un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     */
    void syncPostiStats(String parcheggioId);
}