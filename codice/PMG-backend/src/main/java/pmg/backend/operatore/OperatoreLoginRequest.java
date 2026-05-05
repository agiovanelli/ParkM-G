package pmg.backend.operatore;

/**
 * DTO utilizzato per restituire i dati del login di un operatore.
 *
 * Include il nome della struttura e l'username associato.
 *
 * @param nomeStruttura nome della struttura
 * @param username nome dell'operatore
 */
public record OperatoreLoginRequest(
        String nomeStruttura,
        String username
) {}
