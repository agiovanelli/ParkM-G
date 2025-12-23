package pmg.backend.analitiche;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AnaliticheRepository extends MongoRepository<Analitiche, String> {

	List<Analitiche> findByParcheggioIdAndOperatoreIdAndTipo(String parcheggioId, String operatoreId, String tipo);

    // Recupera un record per nomeAnalitica
    Optional<Analitiche> findByNomeAnalitica(String tipo);

    // Recupera direttamente il campo 'valore' di un record
    Optional<String> findValoreByNomeAnalitica(String tipo);
}
