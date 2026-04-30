package pmg.backend.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per la gestione dei log.
 *
 * Espone endpoint per creare, recuperare e aggiornare
 * i log associati alle analitiche.
 */
@RestController
@RequestMapping("/api/log")
public class LogController {

    /** Servizio per la gestione dei log. */
    private final LogService service;

    /**
     * Crea una nuova istanza del controller dei log.
     *
     * @param service servizio per le operazioni sui log
     */
    @Autowired
    public LogController(LogService service) {
        this.service = service;
    }

    /**
     * Crea un nuovo log a partire dai dati ricevuti.
     *
     * @param request dati necessari per la creazione del log
     * @return risposta contenente i dati del log creato
     */
    @PostMapping
    public LogResponse creaLog(@RequestBody LogRequest request) {
        Log saved = service.salvaLog(request);
        return new LogResponse(
                saved.getId(),
                saved.getAnaliticaId(),
                saved.getTipo().name(),
                saved.getTitolo(),
                saved.getDescrizione(),
                saved.getData(),
                saved.getSeverita().name()
        );
    }

    /**
     * Recupera tutti i log associati a un'analitica.
     *
     * @param analiticaId identificativo dell'analitica
     * @return lista dei log associati all'analitica indicata
     */
    @GetMapping("/analitiche/{analiticaId}/log")
    public List<LogResponse> getLogByAnaliticaId(@PathVariable String analiticaId) {
        return service.getLogByAnaliticaId(analiticaId).stream()
                .map(log -> new LogResponse(
                        log.getId(),
                        log.getAnaliticaId(),
                        log.getTipo().name(),
                        log.getTitolo(),
                        log.getDescrizione(),
                        log.getData(),
                        log.getSeverita().name()
                ))
                .toList();
    }

    /**
     * Recupera i log di un'analitica filtrandoli per categoria.
     *
     * @param analiticaId identificativo dell'analitica
     * @param tipo categoria dei log da recuperare
     * @return lista dei log associati all'analitica e alla categoria indicate
     */
    @GetMapping("/analitiche/{analiticaId}/tipo/{tipo}")
    public List<LogResponse> getLogByAnaliticaIdAndTipo(
            @PathVariable String analiticaId,
            @PathVariable String tipo) {
        return service.getLogByAnaliticaIdAndTipo(analiticaId, tipo).stream()
                .map(log -> new LogResponse(
                        log.getId(),
                        log.getAnaliticaId(),
                        log.getTipo().name(),
                        log.getTitolo(),
                        log.getDescrizione(),
                        log.getData(),
                        log.getSeverita().name()
                ))
                .toList();
    }
    
    /**
     * Aggiorna il livello di severità di un log.
     *
     * @param id identificativo del log
     * @param severity nuovo livello di severità
     * @return log aggiornato
     */
    @PutMapping("/{id}/severity")
    public Log aggiornaSeverity(
            @PathVariable String id,
            @RequestParam LogSeverità severity) {

        Log log = service.getLogById(id)
                .orElseThrow(() -> new RuntimeException("Log non trovato"));

        log.setSeverita(severity);

        return service.salvaLog1(log);
    }
    
    /**
     * Aggiorna la categoria di un log.
     *
     * @param id identificativo del log
     * @param category nuova categoria del log
     * @return log aggiornato
     */
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