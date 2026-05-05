package pmg.backend.operatore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST per la gestione degli operatori.
 *
 * Espone endpoint per l'autenticazione degli operatori
 * e le operazioni a essi associate.
 */
@RestController
@RequestMapping("/api/operatori")
public class OperatoreController {

    /** Logger per il tracciamento delle richieste HTTP. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatoreController.class);

    /** Servizio per la gestione degli operatori. */
    private final OperatoreService operatoreService;

    /**
     * Crea una nuova istanza del controller degli operatori.
     *
     * @param operatoreService servizio per le operazioni sugli operatori
     */
    public OperatoreController(OperatoreService operatoreService) {
        this.operatoreService = operatoreService;
    }

    /**
     * Effettua il login di un operatore.
     *
     * @param req dati di autenticazione dell'operatore
     * @return risposta contenente le informazioni dell'operatore autenticato
     */
    @PostMapping("/login")
    public ResponseEntity<OperatoreResponse> login(@RequestBody OperatoreLoginRequest req) {
        LOGGER.info("HTTP POST /api/operatori/login chiamato");
        OperatoreResponse resp = operatoreService.login(req);
        return ResponseEntity.ok(resp);
    }
}