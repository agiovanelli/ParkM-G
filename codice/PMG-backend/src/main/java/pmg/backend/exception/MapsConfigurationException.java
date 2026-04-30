package pmg.backend.exception;

/**
 * Eccezione utilizzata per indicare un errore di configurazione
 * relativo al servizio Maps.
 *
 * Viene lanciata quando mancano o non sono validi i parametri
 * necessari per utilizzare il servizio Maps.
 */
@SuppressWarnings("serial")
public class MapsConfigurationException extends RuntimeException {
    
    /**
     * Crea una nuova eccezione di configurazione per il servizio Maps.
     *
     * @param message messaggio descrittivo dell'errore
     */
    public MapsConfigurationException(String message) {
        super(message);
    }
}