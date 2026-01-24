
package pmg.backend.prenotazione;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PrenotazioneRepository extends MongoRepository<Prenotazione, String> {
    List<Prenotazione> findByUtenteId(String utenteId);
    List<Prenotazione> findByStatoAndOrarioBefore(StatoPrenotazione stato, LocalDateTime orario);
    Optional<Prenotazione> findByCodiceQr(String codiceQr);
}
