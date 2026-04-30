package pmg.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Gestore globale delle eccezioni dell'applicazione.
 *
 * Intercetta le eccezioni lanciate dai controller e restituisce
 * risposte di errore uniformi.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gestisce le eccezioni relative a risorse non trovate.
     *
     * @param e eccezione generata
     * @return risposta HTTP con stato 404
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Gestisce le eccezioni relative a richieste non valide.
     *
     * @param e eccezione generata
     * @return risposta HTTP con stato 400
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Gestisce le eccezioni relative a conflitti tra risorse.
     *
     * @param e eccezione generata
     * @return risposta HTTP con stato 409
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Gestisce le eccezioni generiche non previste.
     *
     * @param e eccezione generata
     * @return risposta HTTP con stato 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
    	e.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno");
    }
    
    /**
     * Gestisce gli errori relativi alla comunicazione con il servizio Maps.
     *
     * @param e eccezione generata
     * @return risposta HTTP con stato 502
     */
    @ExceptionHandler(MapsApiException.class)
    public ResponseEntity<ErrorResponse> handleMapsApi(MapsApiException e) {
        return buildResponse(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    /**
     * Gestisce gli errori di configurazione relativi al servizio Maps.
     *
     * @param e eccezione generata
     * @return risposta HTTP con stato 500
     */
    @ExceptionHandler(MapsConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleMapsConfig(MapsConfigurationException e) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * Costruisce una risposta di errore standard.
     *
     * @param status stato HTTP da restituire
     * @param message messaggio descrittivo dell'errore
     * @return risposta HTTP contenente i dettagli dell'errore
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message
        );
        return new ResponseEntity<>(error, status);
    }
}