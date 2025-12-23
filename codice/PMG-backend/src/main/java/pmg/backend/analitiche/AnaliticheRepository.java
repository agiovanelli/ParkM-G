package pmg.backend.analitiche;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AnaliticheRepository extends MongoRepository<Analitiche, String> {

	List<Analitiche> findByParcheggioIdAndOperatoreIdAndTipo(String parcheggioId, String operatoreId, String tipo);
	
	List<Analitiche> findByParcheggioId(String parcheggioId);

    List<Analitiche> findByParcheggioIdAndTipo(String parcheggioId, String tipo);

    // Recupera un record per nomeAnalitica
    Optional<Analitiche> findByNomeAnalitica(String nomeAnalitica);

    // Recupera direttamente il campo 'valore' di un record
    @Query(value = "{ 'nomeAnalitica' : ?0 }", fields = "{ 'valore' : 1, '_id': 0 }")
    Optional<String> findValoreByNomeAnalitica(String nomeAnalitica);
}
