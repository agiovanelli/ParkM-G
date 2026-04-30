package pmg.backend.exception;

@SuppressWarnings("serial")
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message);
    }
}