package pmg.backend.exception;

@SuppressWarnings("serial")
public abstract class ApiException extends RuntimeException {

    protected ApiException(String message) {
        super(message);
    }
}