package pmg.backend.utente;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UtenteRepository extends MongoRepository<Utente, String> {

    boolean existsByEmail(String email);

    Optional<Utente> findByEmail(String email);

    Optional<Utente> findByEmailAndPassword(String email, String password);
}
