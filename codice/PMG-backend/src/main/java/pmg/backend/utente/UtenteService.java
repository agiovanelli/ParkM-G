package pmg.backend.utente;

import java.util.Map;

/**
 * Servizio per la gestione degli utenti.
 *
 * Definisce le operazioni principali per la registrazione,
 * autenticazione e gestione delle preferenze degli utenti.
 */
public interface UtenteService {

    /**
     * Registra un nuovo utente.
     *
     * @param req dati necessari per la registrazione
     * @return utente registrato
     * @throws IllegalStateException se esiste già un utente con la stessa email
     */
    UtenteResponse registrazione(UtenteRegisterRequest req);

    /**
     * Effettua il login di un utente.
     *
     * @param req dati di autenticazione
     * @return utente autenticato
     * @throws IllegalArgumentException se le credenziali non sono valide
     */
    UtenteResponse login(UtenteLoginRequest req);

    /**
     * Aggiorna le preferenze di un utente.
     *
     * @param utenteId identificativo dell'utente
     * @param preferenze mappa delle preferenze
     * @throws IllegalArgumentException se l'utente non esiste
     */
    void aggiornaPreferenze(String utenteId, Map<String, String> preferenze);

    /**
     * Recupera le preferenze di un utente.
     *
     * @param utenteId identificativo dell'utente
     * @return mappa delle preferenze (può essere null se non impostate)
     */
    Map<String, String> getPreferenze(String utenteId);

    /**
     * Elimina un utente dal sistema.
     *
     * @param utenteId identificativo dell'utente
     */
    void delete(String utenteId);
}