package pmg.backend.exception;

/**
 * Eccezione utilizzata per indicare un errore nella comunicazione
 * con il servizio Maps.
 *
 * Viene lanciata quando una richiesta verso il servizio Maps
 * non viene completata correttamente.
 */
@SuppressWarnings("serial")
public class MapsApiException extends RuntimeException {
    
    /**
     * Crea una nuova eccezione relativa al servizio Maps.
     *
     * @param message messaggio descrittivo dell'errore
     */
    public MapsApiException(String message) {
        super(message);
    }
}