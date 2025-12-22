package pmg.backend.analitiche;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analitiche")
public class AnaliticheController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AnaliticheController.class);

    private final AnaliticheService analiticheService;

    public AnaliticheController(AnaliticheService analiticheService) {
        this.analiticheService = analiticheService;
    }

    @GetMapping("/getAnalitiche")
    public ResponseEntity<AnaliticheResponse> getAnalitiche() {
        LOGGER.info("HTTP GET /api/analitiche/getAnalitiche chiamato");
        AnaliticheResponse resp = analiticheService.getAnalitiche();
        return ResponseEntity.ok(resp);
    }
}
