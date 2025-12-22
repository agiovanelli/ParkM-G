package pmg.backend.prenotazione;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrenotazioneRepository extends MongoRepository<Prenotazione, String> {
}