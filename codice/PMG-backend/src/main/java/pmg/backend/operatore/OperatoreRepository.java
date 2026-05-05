package pmg.backend.operatore;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository per l'accesso ai dati dell'operatore.
 *
 * Fornisce metodi per recuperare l'operatore in base
 * a parcheggio e username.
 */
public interface OperatoreRepository extends MongoRepository<Operatore, String> {
	
	/**
	 * Recupera l'operatore associato a uno specifico parcheggio e username.
	 *
	 * @param nomeStruttura nome della struttura associata
	 * @param username nome dell'operatore
	 * @return operatore trovato, se presente
	 */
    Optional<Operatore> findByNomeStrutturaAndUsername(String nomeStruttura, String username);
}
