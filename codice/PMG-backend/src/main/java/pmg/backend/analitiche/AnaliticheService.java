package pmg.backend.analitiche;

import org.springframework.stereotype.Service;

/**
 * Servizio per la gestione delle analitiche.
 *
 * Definisce le operazioni principali per recuperare e salvare
 * le analitiche.
 */
@Service
public interface AnaliticheService {
    
    /**
     * Recupera un'analitica tramite il suo identificativo.
     *
     * @param id identificativo dell'analitica
     * @return analitica corrispondente all'identificativo indicato
     */
    Analitiche getById(String id);

    /**
     * Recupera un'analitica associata a un operatore.
     *
     * @param operatoreId identificativo dell'operatore
     * @return analitica associata all'operatore indicato
     */
    Analitiche getByOperatoreId(String operatoreId);
    
    /**
     * Recupera un'analitica associata a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return analitica associata al parcheggio indicato
     */
    Analitiche getByParcheggioId(String parcheggioId);

    /**
     * Salva una nuova analitica a partire dai dati ricevuti.
     *
     * @param request dati necessari per la creazione dell'analitica
     * @return analitica salvata
     */
    Analitiche save(AnaliticheRequest request);
}