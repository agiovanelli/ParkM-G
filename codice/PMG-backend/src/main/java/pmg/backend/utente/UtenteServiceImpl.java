package pmg.backend.utente;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UtenteServiceImpl implements UtenteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtenteServiceImpl.class);

    private final UtenteRepository repository;

    public UtenteServiceImpl(UtenteRepository repository) {
        this.repository = repository;
    }

    @Override
    public UtenteResponse registrazione(UtenteRegisterRequest req) {
        LOGGER.info("Richiesta registrazione utente: email={}", req.email());

        // Controllo se esiste già un utente con la stessa email
        if (repository.existsByEmail(req.email())) {
            LOGGER.warn("Registrazione fallita: email già registrata {}", req.email());
            throw new IllegalStateException("Email già registrata");
        }

        // Genero username come nel tuo codice: nome.cognome
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

    @Override
    public Map<String, String> getPreferenze(String utenteId) {
        LOGGER.info("Recupero preferenze per utente id={}", utenteId);

        Utente entity = repository.findById(utenteId).orElseThrow(() -> {
            LOGGER.warn("Utente non trovato per id={} durante lettura preferenze", utenteId);
            return new IllegalArgumentException("Utente non trovato");
        });

        return entity.getPreferenze();
    }

    @Override
    public void delete(String utenteId) {
        LOGGER.info("Eliminazione utente id={}", utenteId);
        repository.deleteById(utenteId);
    }

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
