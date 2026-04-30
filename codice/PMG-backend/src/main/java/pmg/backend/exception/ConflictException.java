package pmg.backend.exception;

/**
 * Eccezione utilizzata per indicare un conflitto durante l'elaborazione
 * della richiesta.
 *
 * Viene lanciata quando l'operazione richiesta non può essere completata
 * a causa dello stato attuale delle risorse.
 */
@SuppressWarnings("serial")
public class ConflictException extends ApiException {

    /**
     * Crea una nuova eccezione di conflitto.
     *
     * @param message messaggio descrittivo dell'errore
     */
    public ConflictException(String message) {
        super(message);
    }
}