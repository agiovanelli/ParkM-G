package pmg.backend.operatore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OperatoreServiceImpl implements OperatoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatoreServiceImpl.class);

    private final OperatoreRepository repository;

    public OperatoreServiceImpl(OperatoreRepository repository) {
        this.repository = repository;
    }

    @Override
    public OperatoreResponse login(OperatoreLoginRequest req) {
        LOGGER.info("Richiesta login operatore: struttura='{}', username='{}'",
                req.getNomeStruttura(), req.getUsername());

        var entity = repository
                .findByNomeStrutturaAndUsername(req.getNomeStruttura(), req.getUsername())
                .orElseThrow(() -> {
                    LOGGER.warn("Operatore non registrato: struttura='{}', username='{}'",
                            req.getNomeStruttura(), req.getUsername());
                    return new IllegalArgumentException("Operatore non registrato");
                });

        LOGGER.info("Login OK per operatore id={}, struttura='{}', username='{}'",
                entity.getId(), entity.getNomeStruttura(), entity.getUsername());

        return toResponse(entity);
    }

    private OperatoreResponse toResponse(Operatore e) {
        return new OperatoreResponse(
                e.getId(),
                e.getUsername(),      
                e.getNomeStruttura()   
        );
    }
}
