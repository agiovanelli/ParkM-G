package pmg.backend.prenotazione;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<PrenotazioneResponse> validaIngresso(@PathVariable String codiceQr) {
    	PrenotazioneResponse res = prenotazioneService.validaIngresso(codiceQr);
    	return ResponseEntity.ok(res);
    }
    
    @GetMapping("/qr/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> getPrenotazioneByQr(@PathVariable String codiceQr) {
    	PrenotazioneResponse prenotazione = prenotazioneService.getPrenotazioneByQr(codiceQr);
    	return ResponseEntity.ok(prenotazione);
    }
    
    @DeleteMapping("/{prenotazioneId}/utente/{utenteId}")
    public ResponseEntity<PrenotazioneResponse> annullaPrenotazione(
        @PathVariable String prenotazioneId,
        @PathVariable String utenteId
    ) {
    	PrenotazioneResponse res = prenotazioneService.annullaPrenotazione(prenotazioneId, utenteId);
    	return ResponseEntity.ok(res);
    }
    
    @GetMapping("/{id}/calcola-importo")
    public ResponseEntity<Double> getImporto(@PathVariable String id) {
    	double importo = prenotazioneService.calcolaImporto(id);
    	return ResponseEntity.ok(importo);
    }

    @PostMapping("/{id}/paga")
    public ResponseEntity<PrenotazioneResponse> paga(@PathVariable String id, @org.springframework.web.bind.annotation.RequestBody Map<String, Double> payload) {
    	Double importo = payload.get("importo"); // L'operatore o l'app invia l'importo finale
    	PrenotazioneResponse res = prenotazioneService.pagaPrenotazione(id, importo);
    	return ResponseEntity.ok(res);
    }

    @PostMapping("/valida-uscita/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> validaUscita(@PathVariable String codiceQr) {
    	PrenotazioneResponse res = prenotazioneService.validaUscita(codiceQr);
    	return ResponseEntity.ok(res);
    }
    
    @GetMapping("/parcheggio/{parcheggioId}")
    public ResponseEntity<List<PrenotazioneResponse>> getByParcheggio(@PathVariable String parcheggioId) {
        List<PrenotazioneResponse> prenotazioni = prenotazioneService.getByParcheggio(parcheggioId);
        return ResponseEntity.ok(prenotazioni);
    }
    
    @PostMapping("/{id}/parcheggiato")
    public ResponseEntity<PrenotazioneResponse> confermaParcheggio(@PathVariable String id) {
    	PrenotazioneResponse res = prenotazioneService.confermaParcheggio(id);
    	return ResponseEntity.ok(res);
    }
}
