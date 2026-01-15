package pmg.backend.analitiche;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnaliticheRepository extends MongoRepository<Analitiche, String> {

	List<Analitiche> findByParcheggioIdAndOperatoreId(String parcheggioId, String operatoreId);

	Optional<Analitiche> findByOperatoreId(String operatoreId);

    Optional<Analitiche> findByParcheggioId(String parcheggioId);
}
