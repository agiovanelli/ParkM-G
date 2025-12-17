package pmg.backend.utente;

import java.util.Map;

public record UtenteResponse(
        String id,
        String nome,
        String cognome,
        String email,
        String username,
        Map<String, String> preferenze
) {}
