package org.orel.monad;

import org.orel.operator.FaillibleConsumer;
import org.orel.operator.FaillibleFunction;
import org.orel.operator.FaillibleRunnable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

public class Failure<T> implements Try<T> {
    private final Exception exception;

    public Failure(Exception exception) {
        this.exception = exception;
    }

    public Exception exception() {
        return exception;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> thenCompose(FaillibleFunction<? super T, Try<? extends R>, Exception> function) {
        return (Try<R>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<Void> thenRun(FaillibleRunnable<Exception> runnable) {
        return (Try<Void>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<Void> thenAccept(FaillibleConsumer<? super T, Exception> consumer) {
        return (Try<Void>) this;
    }

    @Override
    public T get() {
        throw new NoSuchElementException("No value present", exception);
    }

    @Override
    public T getOrElse(T defaultValue) {
        return defaultValue;
    }

    @Override
    public T getOrElse(Supplier<T> defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier, "Supplier should not be null");
        return defaultValueSupplier.get();
    }

    @Override
    public <R> Try<R> orElseRecover(FaillibleFunction<? super Exception, ? extends R, Exception> function) {
        Objects.requireNonNull(function, "Recover function should not be null");
        try {
            return Try.success(function.apply(exception));
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String toString() {
        return "Failure{" + "exception=" + exception + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Failure<?> failure = (Failure<?>) o;
        return Objects.equals(exception, failure.exception);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exception);
    }
}
