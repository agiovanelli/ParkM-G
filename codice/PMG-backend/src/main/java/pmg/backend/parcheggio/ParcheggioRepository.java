package pmg.backend.parcheggio;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository Spring Data MongoDB per l'accesso ai documenti dei parcheggi.
 *
 * Fornisce le operazioni CRUD standard e la ricerca testuale per area geografica.
 */
public interface ParcheggioRepository extends MongoRepository<Parcheggio, String> {
    List<Parcheggio> findByAreaContainingIgnoreCase(String area);
}
