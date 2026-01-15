package pmg.backend.analitiche;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import pmg.backend.log.LogResponse;
import pmg.backend.log.LogService;

@RestController
@RequestMapping("/api/analitiche")
public class AnaliticheController {
	
	private final AnaliticheService analiticheService;
    private final LogService logService;

    @Autowired
    public AnaliticheController(AnaliticheService analiticheService,
                                LogService logService) {
        this.analiticheService = analiticheService;
        this.logService = logService;
    }

    @GetMapping("/{id}")
    public Analitiche getById(@PathVariable String id) {
        return analiticheService.getById(id);
    }

    @GetMapping("/operatore/{operatoreId}")
    public Analitiche getByOperatoreId(@PathVariable String operatoreId) {
        return analiticheService.getByOperatoreId(operatoreId);
    }

    @PostMapping
    public AnaliticheResponse creaAnalitiche(@RequestBody AnaliticheRequest request) {
        Analitiche saved = analiticheService.save(request);

        return new AnaliticheResponse(
                saved.getId(),
                saved.getParcheggioId(),
                saved.getNomeParcheggio(),
                saved.getOperatoreId(),
                java.util.List.of()
        );
    }

    @GetMapping("/{id}/log")
    public List<LogResponse> getLogByAnaliticaId(@PathVariable String id) {
        return logService.getLogByAnaliticaId(id).stream()
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

    @GetMapping("/{id}/log/{tipo}")
    public List<LogResponse> getLogByAnaliticaIdAndTipo(
            @PathVariable String id,
            @PathVariable String tipo) {
        return logService.getLogByAnaliticaIdAndTipo(id, tipo).stream()
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
}
