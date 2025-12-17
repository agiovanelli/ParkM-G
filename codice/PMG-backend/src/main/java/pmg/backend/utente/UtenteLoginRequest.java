package pmg.backend.utente;

public record UtenteLoginRequest(
        String email,
        String password
) {}
