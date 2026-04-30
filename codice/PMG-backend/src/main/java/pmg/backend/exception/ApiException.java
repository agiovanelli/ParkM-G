package pmg.backend.exception;

/**
 * Eccezione base per la gestione degli errori dell'API.
 *
 * Viene estesa dalle eccezioni specifiche dell'applicazione.
 */
@SuppressWarnings("serial")
public abstract class ApiException extends RuntimeException {

    /**
     * Crea una nuova eccezione API con il messaggio indicato.
     *
     * @param message messaggio descrittivo dell'errore
     */
    protected ApiException(String message) {
        super(message);
    }
}