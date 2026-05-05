package pmg.backend.posto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per la gestione dei posti auto.
 *
 * Espone endpoint per il recupero, la generazione e l'aggiornamento
 * dello stato dei posti all'interno di un parcheggio.
 */
@RestController
@RequestMapping("/api/posti")
public class PostoController {

    /** Servizio per la gestione dei posti. */
    private final PostoService postoService;

    /**
     * Crea una nuova istanza del controller dei posti.
     *
     * @param postoService servizio per le operazioni sui posti
     */
    public PostoController(PostoService postoService) {
        this.postoService = postoService;
    }

    /**
     * Recupera i posti associati a un parcheggio,
     * opzionalmente filtrati per piano.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare (opzionale)
     * @return lista dei posti del parcheggio
     */
    @GetMapping("/parcheggio/{parcheggioId}")
    public ResponseEntity<List<PostoResponse>> getByParcheggio(
            @PathVariable String parcheggioId,
            @RequestParam(required = false) Integer piano
    ) {
        return ResponseEntity.ok(postoService.getPostiByParcheggio(parcheggioId, piano));
    }

    /**
     * Genera i posti per un determinato parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return messaggio di conferma dell'operazione
     */
    @GetMapping("/genera/{parcheggioId}")
    public ResponseEntity<String> generaPosti(@PathVariable String parcheggioId) {
        postoService.generaPosti(parcheggioId);
        return ResponseEntity.ok("Posti generati!");
    }

    /**
     * Aggiorna la disponibilità di un posto specifico.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del posto
     * @param numero numero del posto
     * @param disponibile nuovo stato di disponibilità
     * @return posto aggiornato
     */
    @PatchMapping("/parcheggio/{parcheggioId}/piano/{piano}/numero/{numero}/disponibilita")
    public ResponseEntity<PostoResponse> updateDisponibilita(
            @PathVariable String parcheggioId,
            @PathVariable int piano,
            @PathVariable int numero,
            @RequestParam boolean disponibile
    ) {
        return ResponseEntity.ok(
                postoService.aggiornaDisponibilita(parcheggioId, piano, numero, disponibile)
        );
    }

    /**
     * Aggiorna lo stato di disabilitazione di un posto specifico.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del posto
     * @param numero numero del posto
     * @param disabilitato nuovo stato di disabilitazione
     * @return posto aggiornato
     */
    @PatchMapping("/parcheggio/{parcheggioId}/piano/{piano}/numero/{numero}/disabilitato")
    public ResponseEntity<PostoResponse> updateDisabilitato(
            @PathVariable String parcheggioId,
            @PathVariable int piano,
            @PathVariable int numero,
            @RequestParam boolean disabilitato
    ) {
        return ResponseEntity.ok(
                postoService.aggiornaDisabilitato(parcheggioId, piano, numero, disabilitato)
        );
    }
}