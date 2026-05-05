package pmg.backend.prenotazione;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST per la gestione delle prenotazioni.
 *
 * Espone endpoint per la creazione, validazione, pagamento
 * e consultazione delle prenotazioni.
 */
@RestController
@RequestMapping("/api/prenotazioni")
public class PrenotazioneController {

    /** Servizio per la gestione delle prenotazioni. */
    private final PrenotazioneService prenotazioneService;

    /**
     * Crea una nuova istanza del controller delle prenotazioni.
     *
     * @param prenotazioneService servizio per le operazioni sulle prenotazioni
     */
    public PrenotazioneController(PrenotazioneService prenotazioneService) {
        this.prenotazioneService = prenotazioneService;
    }

    /**
     * Recupera lo storico delle prenotazioni di un utente.
     *
     * @param utenteId identificativo dell'utente
     * @return lista delle prenotazioni dell'utente
     */
    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<List<PrenotazioneResponse>> getStorico(@PathVariable String utenteId) {
        List<PrenotazioneResponse> storico = prenotazioneService.getStoricoUtente(utenteId);
        return ResponseEntity.ok(storico);
    }
    
    /**
     * Valida l'ingresso di una prenotazione tramite codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione aggiornata
     */
    @PostMapping("/valida-ingresso/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> validaIngresso(@PathVariable String codiceQr) {
        PrenotazioneResponse res = prenotazioneService.validaIngresso(codiceQr);
        return ResponseEntity.ok(res);
    }
    
    /**
     * Recupera una prenotazione tramite codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione corrispondente
     */
    @GetMapping("/qr/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> getPrenotazioneByQr(@PathVariable String codiceQr) {
        PrenotazioneResponse prenotazione = prenotazioneService.getPrenotazioneByQr(codiceQr);
        return ResponseEntity.ok(prenotazione);
    }
    
    /**
     * Annulla una prenotazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param utenteId identificativo dell'utente
     * @return prenotazione aggiornata
     */
    @DeleteMapping("/{prenotazioneId}/utente/{utenteId}")
    public ResponseEntity<PrenotazioneResponse> annullaPrenotazione(
        @PathVariable String prenotazioneId,
        @PathVariable String utenteId
    ) {
        PrenotazioneResponse res = prenotazioneService.annullaPrenotazione(prenotazioneId, utenteId);
        return ResponseEntity.ok(res);
    }
    
    /**
     * Calcola l'importo di una prenotazione.
     *
     * @param id identificativo della prenotazione
     * @return importo calcolato
     */
    @GetMapping("/{id}/calcola-importo")
    public ResponseEntity<Double> getImporto(@PathVariable String id) {
        double importo = prenotazioneService.calcolaImporto(id);
        return ResponseEntity.ok(importo);
    }

    /**
     * Effettua il pagamento di una prenotazione.
     *
     * @param id identificativo della prenotazione
     * @param payload dati contenenti l'importo da pagare
     * @return prenotazione aggiornata dopo il pagamento
     */
    @PostMapping("/{id}/paga")
    public ResponseEntity<PrenotazioneResponse> paga(
            @PathVariable String id,
            @RequestBody Map<String, Double> payload) {

        Double importo = payload.get("importo");
        PrenotazioneResponse res = prenotazioneService.pagaPrenotazione(id, importo);
        return ResponseEntity.ok(res);
    }

    /**
     * Valida l'uscita di una prenotazione tramite codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione aggiornata
     */
    @PostMapping("/valida-uscita/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> validaUscita(@PathVariable String codiceQr) {
        PrenotazioneResponse res = prenotazioneService.validaUscita(codiceQr);
        return ResponseEntity.ok(res);
    }
    
    /**
     * Recupera le prenotazioni associate a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista delle prenotazioni del parcheggio
     */
    @GetMapping("/parcheggio/{parcheggioId}")
    public ResponseEntity<List<PrenotazioneResponse>> getByParcheggio(@PathVariable String parcheggioId) {
        List<PrenotazioneResponse> prenotazioni = prenotazioneService.getByParcheggio(parcheggioId);
        return ResponseEntity.ok(prenotazioni);
    }
    
    /**
     * Conferma che il veicolo è stato parcheggiato.
     *
     * @param id identificativo della prenotazione
     * @return prenotazione aggiornata
     */
    @PostMapping("/{id}/parcheggiato")
    public ResponseEntity<PrenotazioneResponse> confermaParcheggio(@PathVariable String id) {
        PrenotazioneResponse res = prenotazioneService.confermaParcheggio(id);
        return ResponseEntity.ok(res);
    }
}