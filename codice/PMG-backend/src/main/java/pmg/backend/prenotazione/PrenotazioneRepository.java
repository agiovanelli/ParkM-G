package pmg.backend.prenotazione;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository per l'accesso ai dati delle prenotazioni.
 *
 * Fornisce metodi per recuperare le prenotazioni in base
 * a utente, stato, parcheggio e codice QR.
 */
public interface PrenotazioneRepository extends MongoRepository<Prenotazione, String> {

    /**
     * Recupera tutte le prenotazioni associate a un utente.
     *
     * @param utenteId identificativo dell'utente
     * @return lista delle prenotazioni dell'utente
     */
    List<Prenotazione> findByUtenteId(String utenteId);

    /**
     * Recupera le prenotazioni con uno specifico stato
     * create prima di una determinata data.
     *
     * @param stato stato della prenotazione
     * @param scadenzaArrivo data limite di creazione
     * @return lista delle prenotazioni trovate
     */
    List<Prenotazione> findByStatoAndDataCreazioneBefore(StatoPrenotazione stato, LocalDateTime scadenzaArrivo);

    /**
     * Recupera una prenotazione tramite il codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione trovata, se presente
     */
    Optional<Prenotazione> findByCodiceQr(String codiceQr);
    
    /**
     * Recupera una prenotazione tramite id e utente associato.
     *
     * @param id identificativo della prenotazione
     * @param utenteId identificativo dell'utente
     * @return prenotazione trovata, se presente
     */
    Optional<Prenotazione> findByIdAndUtenteId(String id, String utenteId);
    
    /**
     * Recupera tutte le prenotazioni associate a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista delle prenotazioni del parcheggio
     */
    List<Prenotazione> findByParcheggioId(String parcheggioId);
}