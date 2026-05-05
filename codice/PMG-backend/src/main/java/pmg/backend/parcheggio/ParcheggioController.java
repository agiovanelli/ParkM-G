package pmg.backend.parcheggio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

import java.util.List;

/**
 * Controller REST per la gestione dei parcheggi.
 *
 * Espone endpoint per la ricerca dei parcheggi, la prenotazione,
 * la gestione dello stato di emergenza e il recupero dei posti.
 */
@RestController
@RequestMapping("/api/parcheggi")
public class ParcheggioController {

    /** Logger per il tracciamento delle richieste HTTP. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioController.class);

    /** Servizio per la gestione dei parcheggi. */
    private final ParcheggioService parcheggioService;

    /**
     * Crea una nuova istanza del controller dei parcheggi.
     *
     * @param parcheggioService servizio per le operazioni sui parcheggi
     */
    public ParcheggioController(ParcheggioService parcheggioService) {
        this.parcheggioService = parcheggioService;
    }

    /**
     * Cerca i parcheggi in base all'area indicata.
     *
     * @param area area geografica di ricerca
     * @return lista dei parcheggi trovati
     */
    @GetMapping("/cerca")
    public ResponseEntity<List<ParcheggioResponse>> cerca(@RequestParam String area) {
        LOGGER.info("HTTP GET /api/parcheggi/cerca?area={}", area);
        return ResponseEntity.ok(parcheggioService.cercaPerArea(area));
    }

    /**
     * Effettua una prenotazione per un parcheggio.
     *
     * @param req dati necessari per la prenotazione
     * @return risposta contenente i dettagli della prenotazione
     */
    @PostMapping("/prenota")
    public ResponseEntity<PrenotazioneResponse> prenota(@RequestBody PrenotazioneRequest req) {
        LOGGER.info("Ricevuta richiesta di prenotazione via HTTP POST");
        return ResponseEntity.ok(parcheggioService.effettuaPrenotazione(req));
    }

    /**
     * Recupera i parcheggi vicini a una posizione geografica.
     *
     * @param lat latitudine della posizione
     * @param lng longitudine della posizione
     * @param radius raggio di ricerca (in metri)
     * @return lista dei parcheggi nelle vicinanze
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<ParcheggioResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radius) {

        LOGGER.info("HTTP GET /api/parcheggi/nearby?lat={}&lng={}&radius={}", lat, lng, radius);
        return ResponseEntity.ok(parcheggioService.cercaVicini(lat, lng, radius));
    }

    /**
     * Attiva o disattiva lo stato di emergenza di un parcheggio.
     *
     * @param id identificativo del parcheggio
     * @param attiva true per attivare l'emergenza, false per disattivarla
     * @param motivo motivo dell'attivazione (opzionale)
     * @return risposta HTTP senza contenuto
     */
    @PatchMapping("/{id}/emergenza")
    public ResponseEntity<Void> toggleEmergenza(
            @PathVariable String id,
            @RequestParam boolean attiva,
            @RequestParam(required = false) String motivo) {

        LOGGER.info("Richiesta cambio stato emergenza per parcheggio {}: {}", id, attiva);
        parcheggioService.impostaStatoEmergenza(id, attiva, motivo);
        return ResponseEntity.ok().build();
    }

    /**
     * Recupera un parcheggio tramite il suo identificativo.
     *
     * @param id identificativo del parcheggio
     * @return parcheggio corrispondente all'identificativo indicato
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParcheggioResponse> getById(@PathVariable String id) {
        LOGGER.info("HTTP GET /api/parcheggi/{}", id);
        return ResponseEntity.ok(parcheggioService.getById(id));
    }

    /**
     * Recupera i posti di un parcheggio, opzionalmente filtrati per piano.
     *
     * @param id identificativo del parcheggio
     * @param piano piano da filtrare (opzionale)
     * @return lista dei posti del parcheggio
     */
    @GetMapping("/{id}/posti")
    public ResponseEntity<List<PostoResponse>> getPosti(
            @PathVariable String id,
            @RequestParam(required = false) Integer piano
    ) {
        LOGGER.info("HTTP GET /api/parcheggi/{}/posti?piano={}", id, piano);
        return ResponseEntity.ok(parcheggioService.getPosti(id, piano));
    }
    
    /**
     * Sincronizza lo stato dei posti di un parcheggio.
     *
     * Da utilizzare quando cambia lo stato dei posti o per effettuare
     * un refresh dei dati.
     *
     * @param id identificativo del parcheggio
     * @return risposta HTTP senza contenuto
     */
    @PutMapping("/{id}/sync")
    public ResponseEntity<Void> syncPosti(@PathVariable String id) {
        parcheggioService.syncPostiStats(id);
        return ResponseEntity.ok().build();
    }
}