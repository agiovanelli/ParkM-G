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
}