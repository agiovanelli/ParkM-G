package pmg.backend.analitiche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pmg.backend.parcheggio.Parcheggio;

@RestController
@RequestMapping("/api/analitiche")
public class AnaliticheController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AnaliticheController.class);

    private final AnaliticheService analiticheService;

    public AnaliticheController(AnaliticheService analiticheService) {
        this.analiticheService = analiticheService;
    }

    @GetMapping("/getAnalitiche")
    public ResponseEntity<AnaliticheResponse> getAnalitiche(Parcheggio parcheggio, String operatoreId) {
        LOGGER.info("HTTP GET /api/analitiche/getAnalitiche chiamato");
        AnaliticheResponse resp = analiticheService.getAnalitiche(parcheggio, operatoreId);
        return ResponseEntity.ok(resp);
    }
}
