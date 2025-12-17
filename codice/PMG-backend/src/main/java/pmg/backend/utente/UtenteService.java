package pmg.backend.utente;

import java.util.Map;

public interface UtenteService {

    /**
     * Registrazione di un nuovo utente.
     * Lancia IllegalStateException se esiste già un utente con la stessa email.
     */
    UtenteResponse registrazione(UtenteRegisterRequest req);

    /**
     * Login utente.
     * Lancia IllegalArgumentException se credenziali non valide.
     */
    UtenteResponse login(UtenteLoginRequest req);

    /**
     * Aggiorna le preferenze di un utente.
     * Lancia IllegalArgumentException se l'utente non esiste.
     */
    void aggiornaPreferenze(String utenteId, Map<String, String> preferenze);

    /**
     * Restituisce le preferenze dell'utente (anche null se non impostate).
     */
    Map<String, String> getPreferenze(String utenteId);

    /**
     * Elimina l'utente dal database.
     */
    void delete(String utenteId);
}
