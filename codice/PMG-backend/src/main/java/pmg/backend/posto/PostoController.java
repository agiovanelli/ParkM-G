package pmg.backend.posto;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST per la gestione dei posti embedded nei parcheggi.
 *
 * Espone operazioni di consultazione, generazione e aggiornamento dello stato
 * di un posto senza utilizzare una collezione MongoDB separata.
 */
@RestController
@RequestMapping("/api/posti")
public class PostoController {

    /**
     * Servizio per la gestione dei posti embedded.
     */
    private final PostoService postoService;

    /**
     * Crea una nuova istanza di PostoController con i dati indicati.
     *
     * @param postoService posto service
     */
    public PostoController(PostoService postoService) {
        this.postoService = postoService;
    }

    /**
     * Recupera gli elementi associati al parcheggio indicato.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @return lista dei posti del parcheggio
     */
    @GetMapping("/parcheggio/{parcheggioId}")
    public ResponseEntity<List<PostoResponse>> getByParcheggio(
            @PathVariable String parcheggioId,
            @RequestParam(required = false) Integer piano) {
        return ResponseEntity.ok(
                postoService.getPostiByParcheggio(parcheggioId, piano));
    }

    /**
     * Genera o completa i posti embedded sulla base della configurazione dei piani.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return risposta HTTP senza contenuto
     */
    @PostMapping("/genera/{parcheggioId}")
    public ResponseEntity<Void> generaPosti(@PathVariable String parcheggioId) {
        postoService.generaPosti(parcheggioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mantiene l'endpoint legacy per la generazione dei posti.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return messaggio di conferma
     */
    @GetMapping("/genera/{parcheggioId}")
    public ResponseEntity<String> generaPostiLegacy(
            @PathVariable String parcheggioId) {
        postoService.generaPosti(parcheggioId);
        return ResponseEntity.ok("Posti generati o sincronizzati");
    }

    /**
     * Aggiorna la disponibilità logica di uno specifico posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param disponibile nuovo valore della disponibilità
     * @return posto aggiornato
     */
    @PatchMapping(
            "/parcheggio/{parcheggioId}/piano/{piano}/numero/{numero}/disponibilita")
    public ResponseEntity<PostoResponse> updateDisponibilita(
            @PathVariable String parcheggioId,
            @PathVariable int piano,
            @PathVariable int numero,
            @RequestParam boolean disponibile) {
        return ResponseEntity.ok(
                postoService.aggiornaDisponibilita(
                        parcheggioId,
                        piano,
                        numero,
                        disponibile));
    }

    /**
     * Aggiorna lo stato legacy di disabilitazione di uno specifico posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param disabilitato nuovo valore legacy di disabilitazione
     * @return posto aggiornato
     */
    @PatchMapping(
            "/parcheggio/{parcheggioId}/piano/{piano}/numero/{numero}/disabilitato")
    public ResponseEntity<PostoResponse> updateDisabilitato(
            @PathVariable String parcheggioId,
            @PathVariable int piano,
            @PathVariable int numero,
            @RequestParam boolean disabilitato) {
        return ResponseEntity.ok(
                postoService.aggiornaDisabilitato(
                        parcheggioId,
                        piano,
                        numero,
                        disabilitato));
    }

    /**
     * Aggiorna la messa fuori servizio di uno specifico posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @param fuoriServizio nuovo valore della messa fuori servizio
     * @return posto aggiornato
     */
    @PatchMapping(
            "/parcheggio/{parcheggioId}/slot/{slotId}/fuori-servizio")
    public ResponseEntity<PostoResponse> updateFuoriServizio(
            @PathVariable String parcheggioId,
            @PathVariable String slotId,
            @RequestParam boolean fuoriServizio) {
        return ResponseEntity.ok(
                postoService.aggiornaFuoriServizio(
                        parcheggioId,
                        slotId,
                        fuoriServizio));
    }

    /**
     * Aggiorna lo stato operativo di uno specifico posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @param stato nuovo stato operativo
     * @return posto aggiornato
     */
    @PatchMapping("/parcheggio/{parcheggioId}/slot/{slotId}/stato")
    public ResponseEntity<PostoResponse> updateStato(
            @PathVariable String parcheggioId,
            @PathVariable String slotId,
            @RequestParam StatoPosto stato) {
        return ResponseEntity.ok(
                postoService.aggiornaStato(parcheggioId, slotId, stato));
    }
}
