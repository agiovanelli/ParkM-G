package pmg.backend.analitiche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnaliticheServiceImpl implements AnaliticheService{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnaliticheServiceImpl.class);

    private final AnaliticheRepository repository;

    public AnaliticheServiceImpl(AnaliticheRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Analitiche getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Analitiche non trovata"));
    }

    @Override
    public Analitiche getByOperatoreId(String operatoreId) {
        return repository.findByOperatoreId(operatoreId)
                .orElseThrow(() -> new RuntimeException("Analitiche non trovata"));
    }

    @Override
    public Analitiche save(Analitiche analitiche) {
        return repository.save(analitiche);
    }

}
