package pmg.backend.operatore;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OperatoreRepository extends MongoRepository<Operatore, String> {

    Optional<Operatore> findByNomeStrutturaAndUsername(String nomeStruttura, String username);
}