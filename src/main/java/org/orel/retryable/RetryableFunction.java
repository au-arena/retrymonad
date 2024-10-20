package org.orel.retryable;

import org.orel.RetryConfig;
import org.orel.operator.FaillibleFunction;

/**
 * Repeats the function execution until the execution is successful or the limit of retries has been
 * reached at which point a {@link RetryConfig.MaxRetriesException} is thrown. <br><br>
 *
 * Note: it retries indefinitely if the number of retries in {@link RetryConfig} is not positive.
 **/
public class RetryableFunction<T, R> implements FaillibleFunction<T, R, Exception> {
    private final FaillibleFunction<? super T, ? extends R, Exception> delegate;

    private final RetryConfig retryConfig;

    public RetryableFunction(
            FaillibleFunction<? super T, ? extends R, Exception> function, RetryConfig RetryConfig) {
        this.retryConfig = RetryConfig;
        this.delegate = function;
    }

    @Override
    public R apply(T t) throws Exception {
        var retryBackoff = retryConfig.backoffDelay().get();
        var currentIteration = 0;

        Exception lastException = null;

        while (retryConfig.nbOfRetries() < 1 || currentIteration++ < retryConfig.nbOfRetries()) {
            try {
                return delegate.apply(t);
            } catch (Exception e) {
                lastException = e;
                try {
                    Thread.sleep(retryBackoff);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e1);
                }
            }
        }
        var msg = String.format(
                "Unable to perform the task within %s retries because: %s",
                retryConfig.nbOfRetries(), lastException.getMessage());
        throw new RetryConfig.MaxRetriesException(msg, lastException);
    }
}
