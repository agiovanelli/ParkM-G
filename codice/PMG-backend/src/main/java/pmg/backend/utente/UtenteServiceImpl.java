package pmg.backend.utente;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementazione del servizio per la gestione degli utenti.
 *
 * Utilizza il repository per gestire le operazioni di
 * registrazione, autenticazione e gestione delle preferenze.
 */
@Service
public class UtenteServiceImpl implements UtenteService {

    /** Logger per il tracciamento delle operazioni. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UtenteServiceImpl.class);

    /** Repository per l'accesso ai dati degli utenti. */
    private final UtenteRepository repository;

    /**
     * Crea una nuova istanza del servizio utenti.
     *
     * @param repository repository degli utenti
     */
    public UtenteServiceImpl(UtenteRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra un nuovo utente nel sistema.
     *
     * @param req dati di registrazione dell'utente
     * @return utente registrato sotto forma di response
     * @throws IllegalStateException se l'email è già registrata
     */
    @Override
    public UtenteResponse registrazione(UtenteRegisterRequest req) {
        LOGGER.info("Richiesta registrazione utente: email={}", req.email());

        if (repository.existsByEmail(req.email())) {
            LOGGER.warn("Registrazione fallita: email già registrata {}", req.email());
            throw new IllegalStateException("Email già registrata");
        }

        String username = req.nome() + "." + req.cognome();

        Utente entity = new Utente(
                req.nome(),
                req.cognome(),
                req.email(),
                username,
                req.password()
        );

        Utente salvato = repository.save(entity);

        LOGGER.info("Nuovo utente registrato con id={} email={}", salvato.getId(), salvato.getEmail());

        return toResponse(salvato);
    }

    /**
     * Effettua il login di un utente.
     *
     * @param req dati di login
     * @return utente autenticato sotto forma di response
     * @throws IllegalArgumentException se le credenziali non sono valide
     */
    @Override
    public UtenteResponse login(UtenteLoginRequest req) {
        LOGGER.info("Richiesta login utente: email={}", req.email());

        Utente entity = repository
                .findByEmailAndPassword(req.email(), req.password())
                .orElseThrow(() -> {
                    LOGGER.warn("Login fallito per email={}", req.email());
                    return new IllegalArgumentException("Credenziali non valide");
                });

        LOGGER.info("Login eseguito correttamente per utente id={} email={}",
                entity.getId(), entity.getEmail());

        return toResponse(entity);
    }

    /**
     * Aggiorna le preferenze di un utente.
     *
     * @param utenteId identificativo dell'utente
     * @param preferenze nuove preferenze da salvare
     * @throws IllegalArgumentException se l'utente non esiste
     */
    @Override
    public void aggiornaPreferenze(String utenteId, Map<String, String> preferenze) {
        LOGGER.info("Aggiornamento preferenze per utente id={}", utenteId);

        Utente entity = repository.findById(utenteId).orElseThrow(() -> {
            LOGGER.warn("Utente non trovato per id={} durante aggiornamento preferenze", utenteId);
            return new IllegalArgumentException("Utente non trovato");
        });

        entity.setPreferenze(preferenze);
        repository.save(entity);

        LOGGER.info("Preferenze aggiornate per utente id={}", utenteId);
    }

    /**
     * Recupera le preferenze di un utente.
     *
     * @param utenteId identificativo dell'utente
     * @return mappa delle preferenze dell'utente
     * @throws IllegalArgumentException se l'utente non esiste
     */
    @Override
    public Map<String, String> getPreferenze(String utenteId) {
        LOGGER.info("Recupero preferenze per utente id={}", utenteId);

        Utente entity = repository.findById(utenteId).orElseThrow(() -> {
            LOGGER.warn("Utente non trovato per id={} durante lettura preferenze", utenteId);
            return new IllegalArgumentException("Utente non trovato");
        });

        return entity.getPreferenze();
    }

    /**
     * Elimina un utente dal sistema.
     *
     * @param utenteId identificativo dell'utente da eliminare
     */
    @Override
    public void delete(String utenteId) {
        LOGGER.info("Eliminazione utente id={}", utenteId);
        repository.deleteById(utenteId);
    }

    /**
     * Converte un'entità Utente in UtenteResponse.
     *
     * @param e entità utente
     * @return oggetto response corrispondente
     */
    private UtenteResponse toResponse(Utente e) {
        return new UtenteResponse(
                e.getId(),
                e.getNome(),
                e.getCognome(),
                e.getEmail(),
                e.getUsername(),
                e.getPreferenze()
        );
    }
}