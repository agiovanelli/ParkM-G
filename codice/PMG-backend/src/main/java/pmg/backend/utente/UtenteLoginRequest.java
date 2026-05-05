package pmg.backend.utente;

/**
 * DTO utilizzato per restituire i dati del login di un utente.
 *
 * Include l'email e la password associata.
 *
 * @param email email dell'utente
 * @param password password dell'utente relativa all'email
 */
public record UtenteLoginRequest(
        String email,
        String password
) {}
