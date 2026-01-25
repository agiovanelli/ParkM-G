package pmg.backend.prenotazione;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrenotazioneRepository extends MongoRepository<Prenotazione, String> {
    List<Prenotazione> findByUtenteId(String utenteId);

    List<Prenotazione> findByStatoAndDataCreazioneBefore(StatoPrenotazione stato, LocalDateTime dataCreazione);

    Optional<Prenotazione> findByCodiceQr(String codiceQr);
}
