package pmg.backend.analitiche;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import pmg.backend.log.LogResponse;
import pmg.backend.log.LogService;

/**
 * Controller REST per la gestione delle analitiche.
 *
 * Espone endpoint per recuperare, creare e consultare le analitiche
 * associate a parcheggi, operatori e relativi log.
 */
@RestController
@RequestMapping("/api/analitiche")
public class AnaliticheController {
	
	/** Servizio per la gestione delle analitiche. */
	private final AnaliticheService analiticheService;
    
    /** Servizio per la gestione dei log associati alle analitiche. */
    private final LogService logService;

    /**
     * Crea una nuova istanza del controller delle analitiche.
     *
     * @param analiticheService servizio per le operazioni sulle analitiche
     * @param logService servizio per le operazioni sui log
     */
    @Autowired
    public AnaliticheController(AnaliticheService analiticheService,
                                LogService logService) {
        this.analiticheService = analiticheService;
        this.logService = logService;
    }

    /**
     * Recupera un'analitica tramite il suo identificativo.
     *
     * @param id identificativo dell'analitica
     * @return analitica corrispondente all'identificativo indicato
     */
    @GetMapping("/{id}")
    public Analitiche getById(@PathVariable String id) {
        return analiticheService.getById(id);
    }

    /**
     * Recupera un'analitica associata a un operatore.
     *
     * @param operatoreId identificativo dell'operatore
     * @return analitica associata all'operatore indicato
     */
    @GetMapping("/operatore/{operatoreId}")
    public Analitiche getByOperatoreId(@PathVariable String operatoreId) {
        return analiticheService.getByOperatoreId(operatoreId);
    }

    /**
     * Crea una nuova analitica a partire dai dati ricevuti.
     *
     * @param request dati necessari per la creazione dell'analitica
     * @return risposta contenente i dati dell'analitica creata
     */
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

    /**
     * Recupera tutti i log associati a un'analitica.
     *
     * @param id identificativo dell'analitica
     * @return lista dei log associati all'analitica indicata
     */
    @GetMapping("/{id}/log")
    public List<LogResponse> getLogByAnaliticaId(@PathVariable String id) {
        return logService.getLogByAnaliticaId(id).stream()
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
     * Recupera i log di un'analitica filtrandoli per tipo.
     *
     * @param id identificativo dell'analitica
     * @param tipo tipo di log da recuperare
     * @return lista dei log associati all'analitica e al tipo indicati
     */
    @GetMapping("/{id}/log/{tipo}")
    public List<LogResponse> getLogByAnaliticaIdAndTipo(
            @PathVariable String id,
            @PathVariable String tipo) {
        return logService.getLogByAnaliticaIdAndTipo(id, tipo).stream()
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
     * Recupera un'analitica associata a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return analitica associata al parcheggio indicato
     */
    @GetMapping("/parcheggio/{parcheggioId}")
    public Analitiche getByParcheggioId(@PathVariable String parcheggioId) {
        return analiticheService.getByParcheggioId(parcheggioId);
    }
}