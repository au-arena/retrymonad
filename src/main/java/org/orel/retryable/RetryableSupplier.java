package org.orel.retryable;

import org.orel.RetryConfig;
import org.orel.operator.FaillibleFunction;
import org.orel.operator.FaillibleSupplier;

public class RetryableSupplier<T> implements FaillibleSupplier<T, Exception> {
    protected final FaillibleSupplier<T, Exception> delegate;

    protected final RetryConfig RetryConfig;

    public RetryableSupplier(FaillibleSupplier<T, Exception> delegate, RetryConfig RetryConfig) {
        this.RetryConfig = RetryConfig;
        this.delegate = delegate;
    }

    @Override
    public T get() throws Exception {
        FaillibleFunction<Void, T, Exception> supplier = ignored -> delegate.get();

        return new RetryableFunction<>(supplier, RetryConfig).apply(null);
    }
}
