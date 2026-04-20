package pmg.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound() {
        NotFoundException ex = new NotFoundException("Not found");

        ResponseEntity<ErrorResponse> res = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertEquals(404, res.getBody().getStatus());
        assertEquals("Not Found", res.getBody().getError());
        assertEquals("Not found", res.getBody().getMessage());
    }

    @Test
    void handleBadRequest() {
        BadRequestException ex = new BadRequestException("Bad request");

        ResponseEntity<ErrorResponse> res = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals(400, res.getBody().getStatus());
        assertEquals("Bad Request", res.getBody().getError());
        assertEquals("Bad request", res.getBody().getMessage());
    }

    @Test
    void handleConflict() {
        ConflictException ex = new ConflictException("Conflict");

        ResponseEntity<ErrorResponse> res = handler.handleConflict(ex);

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertEquals(409, res.getBody().getStatus());
        assertEquals("Conflict", res.getBody().getError());
        assertEquals("Conflict", res.getBody().getMessage());
    }

    @Test
    void handleGeneric() {
        Exception ex = new RuntimeException("Boom");

        ResponseEntity<ErrorResponse> res = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals(500, res.getBody().getStatus());
        assertEquals("Internal Server Error", res.getBody().getError());
        assertEquals("Errore interno", res.getBody().getMessage());
    }

    @Test
    void handleMapsApi() {
        MapsApiException ex = new MapsApiException("Maps error");

        ResponseEntity<ErrorResponse> res = handler.handleMapsApi(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, res.getStatusCode());
        assertEquals(502, res.getBody().getStatus());
        assertEquals("Bad Gateway", res.getBody().getError());
        assertEquals("Maps error", res.getBody().getMessage());
    }

    @Test
    void handleMapsConfig() {
        MapsConfigurationException ex = new MapsConfigurationException("Config error");

        ResponseEntity<ErrorResponse> res = handler.handleMapsConfig(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals(500, res.getBody().getStatus());
        assertEquals("Internal Server Error", res.getBody().getError());
        assertEquals("Config error", res.getBody().getMessage());
    }
}