package pmg.backend.posto;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostoRepository extends MongoRepository<Posto, String> {

    List<Posto> findByParcheggioId(String parcheggioId);

    List<Posto> findByParcheggioIdAndDisponibileTrue(String parcheggioId);
}