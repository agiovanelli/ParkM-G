package pmg.backend.operatore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operatori")
public class OperatoreController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatoreController.class);

    private final OperatoreService operatoreService;

    public OperatoreController(OperatoreService operatoreService) {
        this.operatoreService = operatoreService;
    }

    @PostMapping("/login")
    public ResponseEntity<OperatoreResponse> login(@RequestBody OperatoreLoginRequest req) {
        LOGGER.info("HTTP POST /api/operatori/login chiamato");
        OperatoreResponse resp = operatoreService.login(req);
        return ResponseEntity.ok(resp);
    }
}
