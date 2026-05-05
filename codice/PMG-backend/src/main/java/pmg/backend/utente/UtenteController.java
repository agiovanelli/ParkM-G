package pmg.backend.utente;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST per la gestione degli utenti.
 *
 * Espone endpoint per la registrazione, autenticazione,
 * gestione delle preferenze e operazioni sugli utenti.
 */
@RestController
@RequestMapping("/api/utenti")
// @CrossOrigin(origins = "http://localhost:XXXXX") // se serve per Flutter Web
public class UtenteController {

    /** Logger per il tracciamento delle richieste HTTP. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UtenteController.class);

    /** Servizio per la gestione degli utenti. */
    private final UtenteService utenteService;

    /**
     * Crea una nuova istanza del controller degli utenti.
     *
     * @param utenteService servizio per le operazioni sugli utenti
     */
    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    /**
     * Registra un nuovo utente.
     *
     * @param req dati necessari per la registrazione
     * @return risposta contenente i dati dell'utente registrato
     */
    @PostMapping("/registrazione")
    public ResponseEntity<UtenteResponse> registrazione(@RequestBody UtenteRegisterRequest req) {
        LOGGER.info("HTTP POST /api/utenti/registrazione");
        UtenteResponse resp = utenteService.registrazione(req);
        return ResponseEntity.ok(resp);
    }

    /**
     * Effettua il login di un utente.
     *
     * @param req dati di autenticazione dell'utente
     * @return risposta contenente i dati dell'utente autenticato
     */
    @PostMapping("/login")
    public ResponseEntity<UtenteResponse> login(@RequestBody UtenteLoginRequest req) {
        LOGGER.info("HTTP POST /api/utenti/login");
        UtenteResponse resp = utenteService.login(req);
        return ResponseEntity.ok(resp);
    }

    /**
     * Aggiorna le preferenze di un utente.
     *
     * @param id identificativo dell'utente
     * @param preferenze nuova mappa delle preferenze
     * @return risposta HTTP senza contenuto
     */
    @PutMapping("/{id}/preferenze")
    public ResponseEntity<Void> aggiornaPreferenze(
            @PathVariable String id,
            @RequestBody Map<String, String> preferenze
    ) {
        LOGGER.info("HTTP PUT /api/utenti/{}/preferenze", id);
        utenteService.aggiornaPreferenze(id, preferenze);
        return ResponseEntity.noContent().build();
    }

    /**
     * Recupera le preferenze di un utente.
     *
     * @param id identificativo dell'utente
     * @return mappa delle preferenze dell'utente
     */
    @GetMapping("/{id}/preferenze")
    public ResponseEntity<Map<String, String>> getPreferenze(@PathVariable String id) {
        LOGGER.info("HTTP GET /api/utenti/{}/preferenze", id);
        Map<String, String> pref = utenteService.getPreferenze(id);
        return ResponseEntity.ok(pref);
    }

    /**
     * Elimina un utente.
     *
     * @param id identificativo dell'utente
     * @return risposta HTTP senza contenuto
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        LOGGER.info("HTTP DELETE /api/utenti/{}", id);
        utenteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}