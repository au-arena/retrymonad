package org.orel.retryable;

import org.orel.RetryConfig;
import org.orel.operator.FaillibleConsumer;
import org.orel.operator.FaillibleFunction;

public class RetryableConsumer<T> implements FaillibleConsumer<T, Exception> {
    private final FaillibleConsumer<T, Exception> delegate;

    private final RetryConfig RetryConfig;

    public RetryableConsumer(FaillibleConsumer<T, Exception> delegate, RetryConfig RetryConfig) {
        this.RetryConfig = RetryConfig;
        this.delegate = delegate;
    }

    @Override
    public void accept(T t) throws Exception {
        FaillibleFunction<T, Void, Exception> consumer = x -> {
            delegate.accept(x);
            return null;
        };

        new RetryableFunction<>(consumer, RetryConfig).apply(t);
    }
}
