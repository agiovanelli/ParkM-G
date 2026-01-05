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
    public LogResponse creaLog(@RequestBody LogRequest request) {
        Log saved = service.salvaLog(request);
        return new LogResponse(
                saved.getId(),
                saved.getTipo().name(),
                saved.getTitolo(),
                saved.getDescrizione(),
                saved.getData(),
                saved.getSeverita().name()
        );
    }

    @GetMapping("/analitiche/{analiticaId}/log")
    public List<LogResponse> getLogByAnaliticaId(@PathVariable String analiticaId) {
        return service.getLogByAnaliticaId(analiticaId).stream()
                .map(log -> new LogResponse(
                        log.getId(),
                        log.getTipo().name(),
                        log.getTitolo(),
                        log.getDescrizione(),
                        log.getData(),
                        log.getSeverita().name()
                ))
                .toList();
    }

    @GetMapping("/analitiche/{analiticaId}/tipo/{tipo}")
    public List<LogResponse> getLogByAnaliticaIdAndTipo(
            @PathVariable String analiticaId,
            @PathVariable String tipo) {
        return service.getLogByAnaliticaIdAndTipo(analiticaId, tipo).stream()
                .map(log -> new LogResponse(
                        log.getId(),
                        log.getTipo().name(),
                        log.getTitolo(),
                        log.getDescrizione(),
                        log.getData(),
                        log.getSeverita().name()
                ))
                .toList();
    }
    
    @PutMapping("/{id}/severity")
    public Log aggiornaSeverity(
            @PathVariable String id,
            @RequestParam LogSeverità severity) {

        Log log = service.getLogById(id)
                .orElseThrow(() -> new RuntimeException("Log non trovato"));

        log.setSeverita(severity);

        return service.salvaLog1(log);
    }
    
    @PutMapping("/{id}/category")
    public Log aggiornaCategory(
            @PathVariable String id,
            @RequestParam LogCategoria category) {

        Log log = service.getLogById(id)
                .orElseThrow(() -> new RuntimeException("Log non trovato"));

        log.setTipo(category);

        return service.salvaLog1(log);
    }

}