package pmg.backend.parcheggio;

import java.util.List;
import java.util.Map;

import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

/**
 * Servizio applicativo per la gestione dei parcheggi.
 *
 * Definisce le operazioni di ricerca, prenotazione, gestione dell'emergenza,
 * assegnazione dei posti e recupero della mappa logica.
 */
public interface ParcheggioService {

    /**
     * Cerca i parcheggi presenti nell'area indicata.
     *
     * @param area area geografica di ricerca
     * @return lista dei parcheggi trovati
     */
    List<ParcheggioResponse> cercaPerArea(String area);

    /**
     * Assegna un posto idoneo e crea una nuova prenotazione.
     *
     * @param req dati della richiesta di prenotazione
     * @return prenotazione creata
     */
    PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req);

    /**
     * Cerca e ordina i parcheggi vicini a una posizione geografica.
     *
     * @param lat latitudine del punto di ricerca
     * @param lng longitudine del punto di ricerca
     * @param radius raggio massimo di ricerca in metri
     * @return lista dei parcheggi ordinati per distanza
     */
    List<ParcheggioResponse> cercaVicini(double lat, double lng, double radius);

    /**
     * Aggiorna lo stato di emergenza e registra l'eventuale allarme.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param stato nuovo stato operativo
     * @param motivo motivo dell'emergenza, se disponibile
     */
    void impostaStatoEmergenza(
            String parcheggioId,
            boolean stato,
            String motivo);

    /**
     * Seleziona e prenota il posto più adatto alle preferenze dell'utente.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param preferenze preferenze dell'utente
     * @return posto assegnato oppure {@code null} se non disponibile
     */
    Posto assegnaPostoOttimale(
            String parcheggioId,
            Map<String, String> preferenze);

    /**
     * Recupera un parcheggio tramite il relativo identificativo.
     *
     * @param id identificativo della risorsa
     * @return parcheggio richiesto
     */
    ParcheggioResponse getById(String id);

    /**
     * Recupera la mappa logica completa del parcheggio.
     *
     * @param id identificativo della risorsa
     * @return mappa logica completa
     */
    MappaParcheggioResponse getMappa(String id);

    /**
     * Recupera i posti di un parcheggio, eventualmente filtrati per piano.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @return elenco dei posti
     */
    List<PostoResponse> getPosti(String parcheggioId, Integer piano);

    /**
     * Sincronizza i contatori del parcheggio con lo stato dei posti embedded.
     *
     * @param parcheggioId identificativo del parcheggio
     */
    void syncPostiStats(String parcheggioId);
}
