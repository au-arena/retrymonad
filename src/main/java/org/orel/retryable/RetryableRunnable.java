package org.orel.retryable;

import org.orel.RetryConfig;
import org.orel.operator.FaillibleFunction;
import org.orel.operator.FaillibleRunnable;

public class RetryableRunnable implements FaillibleRunnable<Exception> {
    private final FaillibleRunnable<Exception> delegate;

    private final RetryConfig retryConfig;

    public RetryableRunnable(FaillibleRunnable<Exception> delegate, RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
        this.delegate = delegate;
    }

    @Override
    public void apply() throws Exception {
        FaillibleFunction<Void, Void, Exception> runnable = ignored -> {
            delegate.apply();
            return null;
        };

        new RetryableFunction<>(runnable, retryConfig).apply(null);
    }
}
