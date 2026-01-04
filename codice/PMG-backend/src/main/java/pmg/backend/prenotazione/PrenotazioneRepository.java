package pmg.backend.prenotazione;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrenotazioneRepository extends MongoRepository<Prenotazione, String> {

    // Spring genererà automaticamente la query filtrata per utenteId
    List<Prenotazione> findByUtenteId(String utenteId);
}