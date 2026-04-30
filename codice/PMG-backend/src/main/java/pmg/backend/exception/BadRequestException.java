package pmg.backend.exception;

/**
 * Eccezione utilizzata per indicare una richiesta non valida.
 *
 * Viene lanciata quando i dati ricevuti dall'API non rispettano
 * i requisiti attesi.
 */
@SuppressWarnings("serial")
public class BadRequestException extends ApiException {

    /**
     * Crea una nuova eccezione per richiesta non valida.
     *
     * @param message messaggio descrittivo dell'errore
     */
    public BadRequestException(String message) {
        super(message);
    }
}