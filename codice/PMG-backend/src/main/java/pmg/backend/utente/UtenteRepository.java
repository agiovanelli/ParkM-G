package pmg.backend.utente;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtenteRepository extends MongoRepository<Utente, String> {

    boolean existsByEmail(String email);
    
    Optional<Utente> findById(String id);

    Optional<Utente> findByEmail(String email);

    Optional<Utente> findByEmailAndPassword(String email, String password);
}
