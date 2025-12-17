package pmg.backend.operatore;

// richiesta di login operatore
public record OperatoreLoginRequest(
        String nomeStruttura,
        String username
) { }
