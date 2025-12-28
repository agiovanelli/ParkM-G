package pmg.backend.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/log")
public class LogController {

    private final LogService service;

    @Autowired
    public LogController(LogService service) {
        this.service = service;
    }

    @PostMapping
    public Log creaLog(@RequestBody Log logEvento) {
        return service.salvaLog(logEvento);
    }

    @GetMapping("/analitiche/{analiticaId}/log")
    public List<Log> getLogByAnaliticaId(@PathVariable String analiticaId) {
        return service.getLogByAnaliticaId(analiticaId);
    }

    @GetMapping("/analitiche/{analiticaId}/tipo/{tipo}")
    public List<Log> getLogByAnaliticaIdAndTipo(
            @PathVariable String analiticaId,
            @PathVariable String tipo) {
        return service.getLogByAnaliticaIdAndTipo(analiticaId, tipo);
    }
    
    @PutMapping("/{id}/severity")
    public Log aggiornaSeverity(
            @PathVariable String id,
            @RequestParam LogSeverità severity) {

        Log log = service.getLogById(id)
                .orElseThrow(() -> new RuntimeException("Log non trovato"));

        log.setSeverità(severity);

        return service.salvaLog(log);
    }

}