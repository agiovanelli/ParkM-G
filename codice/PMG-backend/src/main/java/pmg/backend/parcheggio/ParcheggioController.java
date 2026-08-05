package pmg.backend.parcheggio;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

/**
 * Controller REST per la gestione dei parcheggi.
 *
 * Espone endpoint per ricerca, prenotazione, stato di emergenza, recupero dei
 * dati sintetici e caricamento della mappa logica completa del parcheggio.
 */
@RestController
@RequestMapping("/api/parcheggi")
public class ParcheggioController {

    /**
     * Logger utilizzato per tracciare le operazioni applicative.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParcheggioController.class);

    /**
     * Servizio per le operazioni sui parcheggi.
     */
    private final ParcheggioService parcheggioService;

    /**
     * Crea una nuova istanza di ParcheggioController con i dati indicati.
     *
     * @param parcheggioService parcheggio service
     */
    public ParcheggioController(ParcheggioService parcheggioService) {
        this.parcheggioService = parcheggioService;
    }

    /**
     * Cerca i parcheggi che corrispondono all'area indicata.
     *
     * @param area area geografica di ricerca
     * @return lista dei parcheggi corrispondenti
     */
    @GetMapping("/cerca")
    public ResponseEntity<List<ParcheggioResponse>> cerca(
            @RequestParam String area) {
        LOGGER.info("HTTP GET /api/parcheggi/cerca?area={}", area);
        return ResponseEntity.ok(parcheggioService.cercaPerArea(area));
    }

    /**
     * Crea una nuova prenotazione per il parcheggio richiesto.
     *
     * @param req dati della richiesta di prenotazione
     * @return prenotazione creata
     */
    @PostMapping("/prenota")
    public ResponseEntity<PrenotazioneResponse> prenota(
            @RequestBody PrenotazioneRequest req) {
        LOGGER.info("Ricevuta richiesta di prenotazione");
        return ResponseEntity.ok(
                parcheggioService.effettuaPrenotazione(req));
    }

    /**
     * Recupera i parcheggi presenti entro il raggio indicato.
     *
     * @param lat latitudine del punto di ricerca
     * @param lng longitudine del punto di ricerca
     * @param radius raggio massimo di ricerca in metri
     * @return lista dei parcheggi vicini
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<ParcheggioResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radius) {
        return ResponseEntity.ok(
                parcheggioService.cercaVicini(lat, lng, radius));
    }

    /**
     * Attiva o disattiva lo stato di emergenza del parcheggio.
     *
     * @param id identificativo della risorsa
     * @param attiva nuovo valore dello stato di emergenza
     * @param motivo motivo dell'emergenza, se disponibile
     * @return risposta HTTP senza contenuto
     */
    @PatchMapping("/{id}/emergenza")
    public ResponseEntity<Void> toggleEmergenza(
            @PathVariable String id,
            @RequestParam boolean attiva,
            @RequestParam(required = false) String motivo) {
        parcheggioService.impostaStatoEmergenza(id, attiva, motivo);
        return ResponseEntity.ok().build();
    }

    /**
     * Recupera un parcheggio tramite il relativo identificativo.
     *
     * @param id identificativo della risorsa
     * @return parcheggio richiesto
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParcheggioResponse> getById(
            @PathVariable String id) {
        return ResponseEntity.ok(parcheggioService.getById(id));
    }

    /**
     * Recupera la mappa logica completa del parcheggio.
     *
     * @param id identificativo della risorsa
     * @return mappa logica completa
     */
    @GetMapping("/{id}/mappa")
    public ResponseEntity<MappaParcheggioResponse> getMappa(
            @PathVariable String id) {
        return ResponseEntity.ok(parcheggioService.getMappa(id));
    }

    /**
     * Recupera i posti di un parcheggio, eventualmente filtrati per piano.
     *
     * @param id identificativo della risorsa
     * @param piano piano da filtrare o modificare
     * @return elenco dei posti
     */
    @GetMapping("/{id}/posti")
    public ResponseEntity<List<PostoResponse>> getPosti(
            @PathVariable String id,
            @RequestParam(required = false) Integer piano) {
        return ResponseEntity.ok(
                parcheggioService.getPosti(id, piano));
    }

    /**
     * Rigenera i dati mancanti e sincronizza le statistiche dei posti.
     *
     * @param id identificativo della risorsa
     * @return risposta HTTP senza contenuto
     */
    @PutMapping("/{id}/sync")
    public ResponseEntity<Void> syncPosti(@PathVariable String id) {
        parcheggioService.syncPostiStats(id);
        return ResponseEntity.ok().build();
    }
}
