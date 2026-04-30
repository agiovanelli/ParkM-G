package pmg.backend.log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementazione del servizio per la gestione dei log.
 *
 * Utilizza il repository per salvare, recuperare e filtrare
 * i dati dei log.
 */
@Service
public class LogServiceImpl implements LogService{
	
	/** Repository per l'accesso ai dati dei log. */
	private final LogRepository repository;

    /**
     * Crea una nuova istanza del servizio dei log.
     *
     * @param repository repository dei log
     */
    @Autowired
    public LogServiceImpl(LogRepository repository) {
        this.repository = repository;
    }

    /**
     * Recupera un log tramite il suo identificativo.
     *
     * @param id identificativo del log
     * @return log trovato, se presente
     */
    @Override
    public Optional<Log> getLogById(String id) {
        return repository.findById(id);
    }
    
    /**
     * Salva un log già esistente o modificato.
     *
     * @param log log da salvare
     * @return log salvato
     */
    @Override
    public Log salvaLog1(Log log) {
        log.setData(LocalDateTime.now());
        return repository.save(log);
    }
    
    /**
     * Salva un nuovo log a partire dai dati ricevuti.
     *
     * @param request dati necessari per la creazione del log
     * @return log salvato
     */
    public Log salvaLog(LogRequest request) {
        Log entity = new Log();

        entity.setAnaliticaId(request.analiticaId());
        entity.setTipo(request.tipo());
        entity.setSeverita(request.severita());
        entity.setTitolo(request.titolo());
        entity.setDescrizione(request.descrizione());
        entity.setData(request.data());

        return repository.save(entity);
    }

    /**
     * Recupera tutti i log associati a un'analitica ordinati per data decrescente.
     *
     * @param analiticaId identificativo dell'analitica
     * @return lista dei log associati ordinati dal più recente al meno recente
     */
    @Override
    public List<Log> getLogByAnaliticaId(String analiticaId) {
        return repository.findByAnaliticaIdOrderByDataDesc(analiticaId);
    }

    /**
     * Recupera i log associati a un'analitica filtrandoli per categoria.
     *
     * @param analiticaId identificativo dell'analitica
     * @param tipo categoria dei log da recuperare
     * @return lista dei log associati all'analitica e alla categoria indicate
     */
    @Override
    public List<Log> getLogByAnaliticaIdAndTipo(String analiticaId, String tipo) {
        return repository.findByAnaliticaIdAndTipo(analiticaId, tipo);
    }
}