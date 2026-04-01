package pmg.backend.posto;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PostoRepository extends MongoRepository<Posto, String> {

    List<Posto> findByParcheggioIdOrderByPianoAscNumeroAsc(String parcheggioId);

    List<Posto> findByParcheggioIdAndPianoOrderByNumeroAsc(String parcheggioId, int piano);

    Optional<Posto> findByParcheggioIdAndPianoAndNumero(String parcheggioId, int piano, int numero);
    
    Optional<Posto> findByIdAndParcheggioId(String id, String parcheggioId);
}