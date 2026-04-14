package pmg.backend.exception;

@SuppressWarnings("serial")
public class NotFoundException extends ApiException {

	public NotFoundException(String message) {
        super(message);
    }
}