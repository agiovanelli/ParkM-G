package pmg.backend.parcheggio;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ParcheggioRepository extends MongoRepository<Parcheggio, String> {
    
    // Ricerca i parcheggi che contengono la stringa cercata nell'area (case-insensitive)
    List<Parcheggio> findByAreaContainingIgnoreCase(String area);
}