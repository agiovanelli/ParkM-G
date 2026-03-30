package pmg.backend.posto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posti")
public class PostoController {
	@Autowired
    private final PostoService postoService;

    public PostoController(PostoService postoService) {
        this.postoService = postoService;
    }

    @GetMapping("/{id}")
    public List<PostoResponse> getById(@PathVariable String postoId) {
        return postoService.getPostiByParcheggio(postoId);
    }
    
    @GetMapping("/genera/{parcheggioId}")
    public String generaPosti(@PathVariable String parcheggioId) {
        postoService.generaPosti(parcheggioId);
        return "Posti generati!";
    }
}