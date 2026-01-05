package pmg.backend.log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl implements LogService{
	
	private final LogRepository repository;

    @Autowired
    public LogServiceImpl(LogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Log> getLogById(String id) {
        return repository.findById(id);
    }
    
    @Override
    public Log salvaLog1(Log log) {
        log.setData(LocalDateTime.now());
        return repository.save(log);
    }
    
    public Log salvaLog(LogRequest request) {
        Log entity = new Log();

        entity.setTipo(request.tipo());
        entity.setSeverita(request.severita());
        entity.setTitolo(request.titolo());
        entity.setDescrizione(request.descrizione());
        entity.setData(request.data());

        return repository.save(entity);
    }

    @Override
    public List<Log> getLogByAnaliticaId(String analiticaId) {
        return repository.findByAnaliticaIdOrderByDataDesc(analiticaId);
    }

    @Override
    public List<Log> getLogByAnaliticaIdAndTipo(String analiticaId, String tipo) {
        return repository.findByAnaliticaIdAndTipo(analiticaId, tipo);
    }
}
