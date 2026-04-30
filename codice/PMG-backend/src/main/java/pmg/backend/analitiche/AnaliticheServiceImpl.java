package pmg.backend.analitiche;

import org.springframework.stereotype.Service;

/**
 * Implementazione del servizio per la gestione delle analitiche.
 *
 * Utilizza il repository per recuperare e salvare i dati
 * delle analitiche.
 */
@Service
public class AnaliticheServiceImpl implements AnaliticheService{

	/** Repository per l'accesso ai dati delle analitiche. */
	private final AnaliticheRepository repository;

    /**
     * Crea una nuova istanza del servizio delle analitiche.
     *
     * @param repository repository delle analitiche
     */
    public AnaliticheServiceImpl(AnaliticheRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Recupera un'analitica tramite il suo identificativo.
     *
     * @param id identificativo dell'analitica
     * @return analitica corrispondente all'identificativo indicato
     */
    @Override
    public Analitiche getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Analitiche non trovata"));
    }

    /**
     * Recupera un'analitica associata a un operatore.
     *
     * @param operatoreId identificativo dell'operatore
     * @return analitica associata all'operatore indicato
     */
    @Override
    public Analitiche getByOperatoreId(String operatoreId) {
        return repository.findByOperatoreId(operatoreId)
                .orElseThrow(() -> new RuntimeException("Analitiche non trovata"));
    }

    /**
     * Salva una nuova analitica a partire dai dati ricevuti.
     *
     * @param request dati necessari per la creazione dell'analitica
     * @return analitica salvata
     */
    @Override
    public Analitiche save(AnaliticheRequest request) {
        Analitiche entity = new Analitiche(
                request.parcheggioId(),
                request.nomeParcheggio(),
                request.operatoreId()
        );
        return repository.save(entity);
    }

    /**
     * Recupera un'analitica associata a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return analitica associata al parcheggio indicato
     */
    @Override
    public Analitiche getByParcheggioId(String parcheggioId) {
        return repository.findByParcheggioId(parcheggioId)
                .orElseThrow(() -> new RuntimeException("Analitiche non trovata per parcheggioId: " + parcheggioId));
    }

}