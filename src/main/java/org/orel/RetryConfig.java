package org.orel;

import java.util.function.Supplier;

public record RetryConfig(Supplier<Long> backoffDelay, long nbOfRetries) {

    public RetryConfig(long delayInMillis, long nbOfRetries) {
        this(() -> delayInMillis, nbOfRetries);
    }

    public static class MaxRetriesException extends Exception {

        public MaxRetriesException() {}

        public MaxRetriesException(String message) {
            super(message);
        }

        public MaxRetriesException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
