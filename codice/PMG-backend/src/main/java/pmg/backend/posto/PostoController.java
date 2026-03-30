package pmg.backend.posto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posti")
public class PostoController {

    private final PostoService postoService;

    public PostoController(PostoService postoService) {
        this.postoService = postoService;
    }

    @GetMapping("/parcheggio/{parcheggioId}")
    public ResponseEntity<List<PostoResponse>> getByParcheggio(
            @PathVariable String parcheggioId,
            @RequestParam(required = false) Integer piano
    ) {
        return ResponseEntity.ok(postoService.getPostiByParcheggio(parcheggioId, piano));
    }

    @GetMapping("/genera/{parcheggioId}")
    public ResponseEntity<String> generaPosti(@PathVariable String parcheggioId) {
        postoService.generaPosti(parcheggioId);
        return ResponseEntity.ok("Posti generati!");
    }

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