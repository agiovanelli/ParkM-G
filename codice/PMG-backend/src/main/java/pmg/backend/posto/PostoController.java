package pmg.backend.posto;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posti")
public class PostoController {

    private final PostoService service;

    public PostoController(PostoService service) {
        this.service = service;
    }

    @GetMapping("/parcheggio/{parcheggioId}")
    public List<PostoResponse> getByParcheggio(@PathVariable String parcheggioId) {
        return service.getPostiByParcheggio(parcheggioId);
    }

    @PostMapping
    public PostoResponse create(@RequestBody Posto posto) {
        return service.creaPosto(posto);
    }

    @PatchMapping("/parcheggio/{parcheggioId}/disponibilita")
    public PostoResponse updateDisponibilita(
            @PathVariable String id,
            @RequestParam boolean disponibile) {
        return service.aggiornaDisponibilita(id, disponibile);
    }
    
    @PatchMapping("/parcheggio/{parcheggioId}/disabilitato")
    public PostoResponse updateDisabilitato(
            @PathVariable String id,
            @RequestParam boolean disabilitato) {
        return service.aggiornaDisabilitato(id, disabilitato);
    }
}