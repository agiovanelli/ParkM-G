package pmg.backend.prenotazione;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prenotazioni")
public class PrenotazioneController {

    private final PrenotazioneService prenotazioneService;

    public PrenotazioneController(PrenotazioneService prenotazioneService) {
        this.prenotazioneService = prenotazioneService;
    }

    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<List<PrenotazioneResponse>> getStorico(@PathVariable String utenteId) {
        List<PrenotazioneResponse> storico = prenotazioneService.getStoricoUtente(utenteId);
        return ResponseEntity.ok(storico);
    }
    
    @PostMapping("/valida-ingresso/{codiceQr}")
    public ResponseEntity<?> validaIngresso(@PathVariable String codiceQr) {
        try {
            PrenotazioneResponse res = prenotazioneService.validaIngresso(codiceQr);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{prenotazioneId}/utente/{utenteId}")
    public ResponseEntity<?> annullaPrenotazione(
        @PathVariable String prenotazioneId,
        @PathVariable String utenteId
    ) {
        try {
            PrenotazioneResponse res = prenotazioneService.annullaPrenotazione(prenotazioneId, utenteId);
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404
        }
    }

}
