package pmg.backend.log;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends MongoRepository<Log, String>{

	List<Log> findByAnaliticaId(String analiticaId);

    List<Log> findByAnaliticaIdAndTipo(String analiticaId, String tipo);

    List<Log> findByAnaliticaIdOrderByDataDesc(String analiticaId);
}
