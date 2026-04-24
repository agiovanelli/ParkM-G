package pmg.backend.exception;

@SuppressWarnings("serial")
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message);
    }
}