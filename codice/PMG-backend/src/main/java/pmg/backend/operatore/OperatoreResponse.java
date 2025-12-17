package pmg.backend.operatore;

// risposta verso il client
public record OperatoreResponse(
        String id,
        String nomeStruttura,
        String username
) { }
