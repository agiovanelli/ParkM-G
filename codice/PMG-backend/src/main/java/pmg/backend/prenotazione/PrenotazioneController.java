package pmg.backend.prenotazione;

import java.util.List;
import java.util.Map;

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
    @GetMapping("/qr/{codiceQr}")
    public ResponseEntity<?> getPrenotazioneByQr(@PathVariable String codiceQr) {
        try {
            PrenotazioneResponse prenotazione = prenotazioneService.getPrenotazioneByQr(codiceQr);
            return ResponseEntity.ok(prenotazione);
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
    
    @GetMapping("/{id}/calcola-importo")
    public ResponseEntity<Double> getImporto(@PathVariable String id) {
        try {
            double importo = prenotazioneService.calcolaImporto(id);
            return ResponseEntity.ok(importo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/paga")
    public ResponseEntity<?> paga(@PathVariable String id, @org.springframework.web.bind.annotation.RequestBody Map<String, Double> payload) {
        try {
            Double importo = payload.get("importo"); // L'operatore o l'app invia l'importo finale
            PrenotazioneResponse res = prenotazioneService.pagaPrenotazione(id, importo);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/valida-uscita/{codiceQr}")
    public ResponseEntity<?> validaUscita(@PathVariable String codiceQr) {
        try {
            PrenotazioneResponse res = prenotazioneService.validaUscita(codiceQr);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

}
