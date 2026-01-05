package pmg.backend.analitiche;

import org.springframework.stereotype.Service;

@Service
public class AnaliticheServiceImpl implements AnaliticheService{

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
    public Analitiche save(AnaliticheRequest request) {
        Analitiche entity = new Analitiche(
                request.parcheggioId(),
                request.nomeParcheggio(),
                request.operatoreId()
        );
        return repository.save(entity);
    }

}
