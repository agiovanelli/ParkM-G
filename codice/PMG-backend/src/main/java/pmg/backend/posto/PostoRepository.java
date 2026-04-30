package pmg.backend.posto;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostoRepository extends MongoRepository<Posto, String> {

    List<Posto> findByParcheggioIdOrderByPianoAscNumeroAsc(String parcheggioId);

    List<Posto> findByParcheggioIdAndPianoOrderByNumeroAsc(String parcheggioId, int piano);

    Optional<Posto> findByParcheggioIdAndPianoAndNumero(String parcheggioId, int piano, int numero);
    
    Optional<Posto> findByIdAndParcheggioId(String id, String parcheggioId);
    
    int countByParcheggioId(String parcheggioId);
    
    int countByParcheggioIdAndDisponibileTrueAndDisabilitatoFalse(String parcheggioId);
}