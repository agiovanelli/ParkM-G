package pmg.backend.exception;

/**
 * Eccezione utilizzata per indicare che una risorsa richiesta
 * non è stata trovata.
 *
 * Viene lanciata quando l'elemento cercato non esiste
 * o non è disponibile.
 */
@SuppressWarnings("serial")
public class NotFoundException extends ApiException {

	/**
	 * Crea una nuova eccezione per risorsa non trovata.
	 *
	 * @param message messaggio descrittivo dell'errore
	 */
	public NotFoundException(String message) {
        super(message);
    }
}