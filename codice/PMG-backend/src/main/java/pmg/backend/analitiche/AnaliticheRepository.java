package pmg.backend.analitiche;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository per l'accesso ai dati delle analitiche.
 *
 * Fornisce metodi per recuperare le analitiche in base
 * a parcheggio e operatore.
 */
@Repository
public interface AnaliticheRepository extends MongoRepository<Analitiche, String> {

	/**
	 * Recupera le analitiche associate a uno specifico parcheggio e operatore.
	 *
	 * @param parcheggioId identificativo del parcheggio
	 * @param operatoreId identificativo dell'operatore
	 * @return lista delle analitiche trovate
	 */
	List<Analitiche> findByParcheggioIdAndOperatoreId(String parcheggioId, String operatoreId);

	/**
	 * Recupera un'analitica associata a uno specifico operatore.
	 *
	 * @param operatoreId identificativo dell'operatore
	 * @return analitica trovata, se presente
	 */
	Optional<Analitiche> findByOperatoreId(String operatoreId);

    /**
     * Recupera un'analitica associata a uno specifico parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return analitica trovata, se presente
     */
    Optional<Analitiche> findByParcheggioId(String parcheggioId);
}