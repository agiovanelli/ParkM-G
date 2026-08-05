package pmg.backend.prenotazione;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository Spring Data MongoDB per l'accesso alle prenotazioni.
 *
 * Fornisce query per utente, parcheggio, codice QR, stato e scadenza di arrivo.
 */
public interface PrenotazioneRepository
        extends MongoRepository<Prenotazione, String> {

    List<Prenotazione> findByUtenteId(String utenteId);

    List<Prenotazione> findByStatoAndScadenzaArrivoBefore(
            StatoPrenotazione stato,
            LocalDateTime now);

    Optional<Prenotazione> findByCodiceQr(String codiceQr);

    Optional<Prenotazione> findByIdAndUtenteId(
            String id,
            String utenteId);

    List<Prenotazione> findByParcheggioId(String parcheggioId);
}
