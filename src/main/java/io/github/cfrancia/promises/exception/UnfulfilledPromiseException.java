package io.github.cfrancia.promises.exception;

/**
 * Created by cfrancia on 14/08/16.
 */
public class UnfulfilledPromiseException extends RuntimeException {

    public UnfulfilledPromiseException() {
        super();
    }

    public UnfulfilledPromiseException(Throwable cause) {
        super(cause);
    }
}
