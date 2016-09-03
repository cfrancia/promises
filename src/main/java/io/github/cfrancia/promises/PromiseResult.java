package io.github.cfrancia.promises;

import java.util.Objects;

/**
 * Created by cfrancia on 14/08/16.
 */
public interface PromiseResult<T> {

    static <T> PromiseResult<T> ok(T value) {
        return new OkPromiseResult<>(value);
    }

    static <T> PromiseResult<T> error(Exception exception) {
        return new ErrorPromiseResult<>(exception);
    }

    T getValue();

    Exception getException();

    boolean isOk();

    boolean isError();

    class OkPromiseResult<T> implements PromiseResult<T> {
        private final T value;

        private OkPromiseResult(T value) {
            Objects.requireNonNull(value);
            this.value = value;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public Exception getException() {
            throw new NullPointerException();
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public boolean isError() {
            return false;
        }

        @Override
        public String toString() {
            return "PromiseResult[ok]: " + value.toString();
        }
    }

    class ErrorPromiseResult<T> implements PromiseResult<T> {
        private final Exception exception;


        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        private ErrorPromiseResult(Exception exception) {
            Objects.requireNonNull(exception);
            this.exception = exception;
        }

        @Override
        public T getValue() {
            throw new NullPointerException();
        }

        @Override
        public Exception getException() {
            return exception;
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public boolean isError() {
            return true;
        }

        @Override
        public String toString() {
            return "PromiseResult[error]: " + exception.toString();
        }
    }
}
