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

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.eliminaPosto(id);
    }

    @PatchMapping("/{id}/disponibilita")
    public PostoResponse updateDisponibilita(
            @PathVariable String id,
            @RequestParam boolean disponibile) {
        return service.aggiornaDisponibilita(id, disponibile);
    }
}