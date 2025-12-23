package pmg.backend.analitiche;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import pmg.backend.log.Log;
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
    public Analitiche creaAnalitiche(@RequestBody Analitiche analitiche) {
        return analiticheService.save(analitiche);
    }

    @GetMapping("/{id}/log")
    public List<Log> getLogByAnaliticaId(@PathVariable String id) {
        return logService.getLogByAnaliticaId(id);
    }

    @GetMapping("/{id}/log/{tipo}")
    public List<Log> getLogByAnaliticaIdAndTipo(@PathVariable String id,
                                                     @PathVariable String tipo) {
        return logService.getLogByAnaliticaIdAndTipo(id, tipo);
    }
}
