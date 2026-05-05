package pmg.backend.utente;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository per l'accesso ai dati degli utenti.
 *
 * Fornisce metodi per verificare l'esistenza e recuperare
 * gli utenti in base a email, id e credenziali.
 */
@Repository
public interface UtenteRepository extends MongoRepository<Utente, String> {

    /**
     * Verifica se esiste un utente con una determinata email.
     *
     * @param email email dell'utente
     * @return true se esiste un utente con l'email indicata, false altrimenti
     */
    boolean existsByEmail(String email);
    
    /**
     * Recupera un utente tramite il suo identificativo.
     *
     * @param id identificativo dell'utente
     * @return utente trovato, se presente
     */
    Optional<Utente> findById(String id);

    /**
     * Recupera un utente tramite la sua email.
     *
     * @param email email dell'utente
     * @return utente trovato, se presente
     */
    Optional<Utente> findByEmail(String email);

    /**
     * Recupera un utente tramite email e password.
     *
     * @param email email dell'utente
     * @param password password dell'utente
     * @return utente trovato, se presente
     */
    Optional<Utente> findByEmailAndPassword(String email, String password);
}