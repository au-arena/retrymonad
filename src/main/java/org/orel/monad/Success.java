package org.orel.monad;

import org.orel.operator.FaillibleConsumer;
import org.orel.operator.FaillibleFunction;
import org.orel.operator.FaillibleRunnable;

import java.util.Objects;
import java.util.function.Supplier;

public class Success<T> implements Try<T> {
    private final T value;

    public Success(T value) {
        this.value = value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> thenCompose(FaillibleFunction<? super T, Try<? extends R>, Exception> function) {
        Objects.requireNonNull(function, "Function should not be null");
        try {
            return (Try<R>) function.apply(value);
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    @Override
    public Try<Void> thenRun(FaillibleRunnable<Exception> runnable) {
        Objects.requireNonNull(runnable, "Runnable should not be null");
        try {
            runnable.apply();
            return Try.success(null);
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    @Override
    public Try<Void> thenAccept(FaillibleConsumer<? super T, Exception> consumer) {
        Objects.requireNonNull(consumer, "Consumer should not be null");
        try {
            consumer.accept(value);
            return Try.success(null);
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public T getOrElse(T defaultValue) {
        return this.value;
    }

    @Override
    public T getOrElse(Supplier<T> defaultValueSupplier) {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> orElseRecover(FaillibleFunction<? super Exception, ? extends R, Exception> function) {
        Objects.requireNonNull(function, "Recover function should not be null");
        return (Try<R>) this;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public String toString() {
        return "Success{" + "value=" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Success<?> success = (Success<?>) o;
        return Objects.equals(value, success.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
