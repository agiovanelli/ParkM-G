package pmg.backend.exception;

@SuppressWarnings("serial")
public class MapsConfigurationException extends RuntimeException {
    public MapsConfigurationException(String message) {
        super(message);
    }
}