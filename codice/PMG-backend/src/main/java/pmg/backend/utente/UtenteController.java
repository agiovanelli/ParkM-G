package pmg.backend.utente;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utenti")
// @CrossOrigin(origins = "http://localhost:XXXXX") // se serve per Flutter Web
public class UtenteController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtenteController.class);

    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @PostMapping("/registrazione")
    public ResponseEntity<UtenteResponse> registrazione(@RequestBody UtenteRegisterRequest req) {
        LOGGER.info("HTTP POST /api/utenti/registrazione");
        UtenteResponse resp = utenteService.registrazione(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<UtenteResponse> login(@RequestBody UtenteLoginRequest req) {
        LOGGER.info("HTTP POST /api/utenti/login");
        UtenteResponse resp = utenteService.login(req);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}/preferenze")
    public ResponseEntity<Void> aggiornaPreferenze(
            @PathVariable String id,
            @RequestBody Map<String, String> preferenze
    ) {
        LOGGER.info("HTTP PUT /api/utenti/{}/preferenze", id);
        utenteService.aggiornaPreferenze(id, preferenze);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/preferenze")
    public ResponseEntity<Map<String, String>> getPreferenze(@PathVariable String id) {
        LOGGER.info("HTTP GET /api/utenti/{}/preferenze", id);
        Map<String, String> pref = utenteService.getPreferenze(id);
        return ResponseEntity.ok(pref);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        LOGGER.info("HTTP DELETE /api/utenti/{}", id);
        utenteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
