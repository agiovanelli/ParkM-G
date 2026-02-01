package pmg.backend.parcheggio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

import java.util.List;

@RestController

@RequestMapping("/api/parcheggi")
public class ParcheggioController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioController.class);
    private final ParcheggioService parcheggioService;

    public ParcheggioController(ParcheggioService parcheggioService) {
        this.parcheggioService = parcheggioService;
    }

    @GetMapping("/cerca")
    public ResponseEntity<List<ParcheggioResponse>> cerca(@RequestParam String area) {
        LOGGER.info("HTTP GET /api/parcheggi/cerca?area={}", area);
        List<ParcheggioResponse> risultati = parcheggioService.cercaPerArea(area);
        return ResponseEntity.ok(risultati);
    }
    
    @PostMapping("/prenota")
    public ResponseEntity<PrenotazioneResponse> prenota(@RequestBody PrenotazioneRequest req) {
        LOGGER.info("Ricevuta richiesta di prenotazione via HTTP POST");
        
        PrenotazioneResponse response = parcheggioService.effettuaPrenotazione(req);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<List<ParcheggioResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radius) {

        LOGGER.info("HTTP GET /api/parcheggi/nearby?lat={}&lng={}&radius={}", lat, lng, radius);
        List<ParcheggioResponse> risultati = parcheggioService.cercaVicini(lat, lng, radius);
        return ResponseEntity.ok(risultati);
    }

    @PatchMapping("/{id}/emergenza")
    public ResponseEntity<Void> toggleEmergenza(
            @PathVariable String id, 
            @RequestParam boolean attiva,
            @RequestParam(required = false) String motivo) {
        
        LOGGER.info("Richiesta cambio stato emergenza per parcheggio {}: {}", id, attiva);
        parcheggioService.impostaStatoEmergenza(id, attiva, motivo);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ParcheggioResponse> getById(@PathVariable String id) {
        LOGGER.info("HTTP GET /api/parcheggi/{}", id);
        return ResponseEntity.ok(parcheggioService.getById(id));
    }
}