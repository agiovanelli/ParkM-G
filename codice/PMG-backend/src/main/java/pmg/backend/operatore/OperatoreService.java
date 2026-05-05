package pmg.backend.operatore;

/**
 * Servizio per la gestione degli operatori.
 *
 * Definisce le operazioni principali per recuperare e salvare
 * gli operatori.
 */
public interface OperatoreService {

    /**
     * Esegue il login di un operatore dato nome struttura e username.
     *
     * @param req DTO con nomeStruttura e username
     * @return OperatoreResponse con i dati essenziali
     * @throws IllegalArgumentException se l'operatore non è registrato
     */
    OperatoreResponse login(OperatoreLoginRequest req);
}
