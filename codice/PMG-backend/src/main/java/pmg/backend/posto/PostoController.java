package pmg.backend.posto;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posti")
public class PostoController {

    private final PostoService postoService;

    public PostoController(PostoService postoService) {
        this.postoService = postoService;
    }

    @GetMapping("/{id}")
    public List<PostoResponse> getById(@PathVariable String postoId) {
        return postoService.getPostiByParcheggio(postoId);
    }

}