package pmg.backend.operatore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementazione del servizio per la gestione degli operatori.
 *
 * Utilizza il repository per recuperare i dati degli operatori
 * e gestire le operazioni di autenticazione.
 */
@Service
public class OperatoreServiceImpl implements OperatoreService {

    /** Logger per il tracciamento delle operazioni del servizio. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatoreServiceImpl.class);

    /** Repository per l'accesso ai dati degli operatori. */
    private final OperatoreRepository repository;

    /**
     * Crea una nuova istanza del servizio degli operatori.
     *
     * @param repository repository degli operatori
     */
    public OperatoreServiceImpl(OperatoreRepository repository) {
        this.repository = repository;
    }

    /**
     * Effettua il login di un operatore.
     *
     * Verifica l'esistenza dell'operatore in base a nome struttura e username.
     *
     * @param req dati di autenticazione dell'operatore
     * @return risposta contenente i dati dell'operatore autenticato
     * @throws IllegalArgumentException se l'operatore non è registrato
     */
    @Override
    public OperatoreResponse login(OperatoreLoginRequest req) {
        LOGGER.info("Richiesta login operatore: struttura='{}', username='{}'",
                req.nomeStruttura(), req.username());

        var entity = repository
                .findByNomeStrutturaAndUsername(req.nomeStruttura(), req.username())
                .orElseThrow(() -> {
                    LOGGER.warn("Operatore non registrato: struttura='{}', username='{}'",
                            req.nomeStruttura(), req.username());
                    return new IllegalArgumentException("Operatore non registrato");
                });

        LOGGER.info("Login OK per operatore id={}, struttura='{}', username='{}'",
                entity.getId(), entity.getNomeStruttura(), entity.getUsername());

        return toResponse(entity);
    }

    /**
     * Converte un'entità Operatore in un DTO di risposta.
     *
     * @param e entità operatore
     * @return DTO contenente i dati dell'operatore
     */
    private OperatoreResponse toResponse(Operatore e) {
        return new OperatoreResponse(
                e.getId(),
                e.getUsername(),
                e.getNomeStruttura(),
                e.getParcheggioId()
        );
    }
}