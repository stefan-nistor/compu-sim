package ro.uaic.swqual.exception;

public class ParameterException extends RuntimeException {
    public ParameterException(String message) {
        super(message);
    }

    public ParameterException(Throwable cause) {
        super(cause);
    }
}
