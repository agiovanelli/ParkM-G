package pmg.backend.exception;

import java.time.LocalDateTime;

/**
 * DTO utilizzato per restituire una risposta di errore.
 *
 * Contiene le informazioni principali dell'errore generato
 * durante l'elaborazione di una richiesta API.
 */
public class ErrorResponse {

    /** Data e ora in cui si è verificato l'errore. */
    private LocalDateTime timestamp;
    
    /** Codice di stato HTTP associato all'errore. */
    private int status;
    
    /** Nome o descrizione sintetica dell'errore. */
    private String error;
    
    /** Messaggio descrittivo dell'errore. */
    private String message;

    /**
     * Crea una nuova risposta di errore.
     *
     * @param status codice di stato HTTP
     * @param error descrizione sintetica dell'errore
     * @param message messaggio descrittivo dell'errore
     */
    public ErrorResponse(int status, String error, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    /**
     * Restituisce la data e l'ora dell'errore.
     *
     * @return data e ora dell'errore
     */
    public LocalDateTime getTimestamp() { return timestamp; }
    
    /**
     * Restituisce il codice di stato HTTP.
     *
     * @return codice di stato HTTP
     */
    public int getStatus() { return status; }
    
    /**
     * Restituisce la descrizione sintetica dell'errore.
     *
     * @return descrizione sintetica dell'errore
     */
    public String getError() { return error; }
    
    /**
     * Restituisce il messaggio descrittivo dell'errore.
     *
     * @return messaggio descrittivo dell'errore
     */
    public String getMessage() { return message; }
}