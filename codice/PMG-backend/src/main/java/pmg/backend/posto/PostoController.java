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

}