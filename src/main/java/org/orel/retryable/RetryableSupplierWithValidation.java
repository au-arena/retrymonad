package org.orel.retryable;

import org.orel.RetryConfig;
import org.orel.operator.FaillibleFunction;
import org.orel.operator.FaillibleSupplier;

import java.util.function.Predicate;

import static java.lang.String.format;

public class RetryableSupplierWithValidation<T> extends RetryableSupplier<T> implements FaillibleSupplier<T, Exception> {
    private final Predicate<T> validator;

    public RetryableSupplierWithValidation(
            FaillibleSupplier<T, Exception> delegate, RetryConfig RetryConfig, Predicate<T> validator) {
        super(delegate, RetryConfig);
        this.validator = validator;
    }

    @Override
    public T get() throws Exception {
        FaillibleFunction<Void, T, Exception> validatingSupplier = ignored -> {
            T value = delegate.get();
            if (!validator.test(value)) {
                throw new IllegalStateException(format("Supplied value '%s' doesn't satisfy the condition.", value));
            }
            return value;
        };
        return new RetryableFunction<>(validatingSupplier, retryConfig).apply(null);
    }
}
