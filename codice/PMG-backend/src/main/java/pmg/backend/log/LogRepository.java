package pmg.backend.log;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository per l'accesso ai dati dei log.
 *
 * Fornisce metodi per recuperare i log associati
 * a una specifica analitica.
 */
@Repository
public interface LogRepository extends MongoRepository<Log, String>{
	
	/**
	 * Recupera tutti i log associati a un'analitica.
	 *
	 * @param analiticaId identificativo dell'analitica
	 * @return lista dei log trovati
	 */
	List<Log> findByAnaliticaId(String analiticaId);

    /**
     * Recupera i log associati a un'analitica e filtrati per categoria.
     *
     * @param analiticaId identificativo dell'analitica
     * @param tipo categoria del log
     * @return lista dei log trovati
     */
    List<Log> findByAnaliticaIdAndTipo(String analiticaId, String tipo);

    /**
     * Recupera i log associati a un'analitica ordinati per data decrescente.
     *
     * @param analiticaId identificativo dell'analitica
     * @return lista dei log ordinati dal più recente al meno recente
     */
    List<Log> findByAnaliticaIdOrderByDataDesc(String analiticaId);
}