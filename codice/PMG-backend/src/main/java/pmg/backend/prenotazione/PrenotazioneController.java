package pmg.backend.prenotazione;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST per la gestione del ciclo di vita delle prenotazioni.
 *
 * Espone endpoint per storico, validazione di ingresso e uscita, annullamento,
 * pagamento, ricerca tramite QR e conferma del parcheggio.
 */
@RestController
@RequestMapping("/api/prenotazioni")
public class PrenotazioneController {

    /**
     * Servizio per la gestione delle prenotazioni.
     */
    private final PrenotazioneService prenotazioneService;

    /**
     * Crea una nuova istanza di PrenotazioneController con i dati indicati.
     *
     * @param prenotazioneService prenotazione service
     */
    public PrenotazioneController(PrenotazioneService prenotazioneService) {
        this.prenotazioneService = prenotazioneService;
    }

    /**
     * Recupera lo storico delle prenotazioni dell'utente.
     *
     * @param utenteId identificativo dell'utente
     * @return storico delle prenotazioni
     */
    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<List<PrenotazioneResponse>> getStorico(
            @PathVariable String utenteId) {
        return ResponseEntity.ok(
                prenotazioneService.getStoricoUtente(utenteId));
    }

    /**
     * Valida l'ingresso tramite il codice QR della prenotazione.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione aggiornata
     */
    @PostMapping("/valida-ingresso/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> validaIngresso(
            @PathVariable String codiceQr) {
        return ResponseEntity.ok(
                prenotazioneService.validaIngresso(codiceQr));
    }

    /**
     * Recupera una prenotazione tramite il relativo codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione trovata
     */
    @GetMapping("/qr/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> getPrenotazioneByQr(
            @PathVariable String codiceQr) {
        return ResponseEntity.ok(
                prenotazioneService.getPrenotazioneByQr(codiceQr));
    }

    /**
     * Annulla una prenotazione e libera il posto associato.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param utenteId identificativo dell'utente
     * @return prenotazione annullata
     */
    @DeleteMapping("/{prenotazioneId}/utente/{utenteId}")
    public ResponseEntity<PrenotazioneResponse> annullaPrenotazione(
            @PathVariable String prenotazioneId,
            @PathVariable String utenteId) {
        return ResponseEntity.ok(
                prenotazioneService.annullaPrenotazione(
                        prenotazioneId,
                        utenteId));
    }

    /**
     * Calcola l'importo corrente della prenotazione.
     *
     * @param id identificativo della risorsa
     * @return importo calcolato
     */
    @GetMapping("/{id}/calcola-importo")
    public ResponseEntity<Double> getImporto(@PathVariable String id) {
        return ResponseEntity.ok(
                prenotazioneService.calcolaImporto(id));
    }

    /**
     * Registra il pagamento della prenotazione.
     *
     * @param id identificativo della risorsa
     * @param payload dati del pagamento
     * @return prenotazione aggiornata
     */
    @PostMapping("/{id}/paga")
    public ResponseEntity<PrenotazioneResponse> paga(
            @PathVariable String id,
            @RequestBody Map<String, Double> payload) {
        Double importo = payload.get("importo");
        if (importo == null) {
            throw new IllegalArgumentException("Importo obbligatorio");
        }
        return ResponseEntity.ok(
                prenotazioneService.pagaPrenotazione(id, importo));
    }

    /**
     * Valida l'uscita e rende nuovamente libero il posto.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione conclusa
     */
    @PostMapping("/valida-uscita/{codiceQr}")
    public ResponseEntity<PrenotazioneResponse> validaUscita(
            @PathVariable String codiceQr) {
        return ResponseEntity.ok(
                prenotazioneService.validaUscita(codiceQr));
    }

    /**
     * Recupera gli elementi associati al parcheggio indicato.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista dei posti del parcheggio
     */
    @GetMapping("/parcheggio/{parcheggioId}")
    public ResponseEntity<List<PrenotazioneResponse>> getByParcheggio(
            @PathVariable String parcheggioId) {
        return ResponseEntity.ok(
                prenotazioneService.getByParcheggio(parcheggioId));
    }

    /**
     * Conferma che il veicolo ha raggiunto e occupato il posto assegnato.
     *
     * @param id identificativo della risorsa
     * @return prenotazione aggiornata
     */
    @PostMapping("/{id}/parcheggiato")
    public ResponseEntity<PrenotazioneResponse> confermaParcheggio(
            @PathVariable String id) {
        return ResponseEntity.ok(
                prenotazioneService.confermaParcheggio(id));
    }
}
