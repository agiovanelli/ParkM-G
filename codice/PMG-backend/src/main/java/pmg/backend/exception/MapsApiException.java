package pmg.backend.exception;

@SuppressWarnings("serial")
public class MapsApiException extends RuntimeException {
    public MapsApiException(String message) {
        super(message);
    }
}