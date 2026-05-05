package pmg.backend.utente;

/**
 * DTO utilizzato per restituire i dati della registrazione di un utente.
 *
 * Include nome, cognome, email e la password dell'utente.
 *
 * @param nome nome dell'utente
 * @param cognome cognome dell'utente
 * @param email email dell'utente
 * @param password password dell'utente relativa all'email
 */
public record UtenteRegisterRequest(
        String nome,
        String cognome,
        String email,
        String password
) {}
