package pmg.backend.utente;

public record UtenteRegisterRequest(
        String nome,
        String cognome,
        String email,
        String password
) {}
