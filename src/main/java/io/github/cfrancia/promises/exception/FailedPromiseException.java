package io.github.cfrancia.promises.exception;

/**
 * Created by cfrancia on 14/08/16.
 */
public class FailedPromiseException extends RuntimeException {

    public FailedPromiseException(Throwable cause) {
        super(cause);
    }
}
