package org.orel.monad;

import org.orel.RetryConfig;
import org.orel.operator.FaillibleBiFunction;
import org.orel.operator.FaillibleConsumer;
import org.orel.operator.FaillibleFunction;
import org.orel.operator.FaillibleRunnable;
import org.orel.operator.FaillibleSupplier;
import org.orel.retryable.RetryableConsumer;
import org.orel.retryable.RetryableFunction;
import org.orel.retryable.RetryableRunnable;
import org.orel.retryable.RetryableSupplier;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The Try monad is one specific implementation of the monad design pattern used for 1) representing computations that have
 * either failed or succeeded and 2) for coordinating their execution (in a pipelined fashion and optionally retryable). <br><br>
 *
 * Example:
 *
 * <pre>
 *      Supplier<Long> delay = () -> 10L;
 *
 *      RetryConfig retryConfig = new RetryConfig( delay, 5 );
 *
 *      Try<Integer> result = Try.run( () -> 2 )
 *          .thenApply( x -> x + 1, retryConfig )
 *          .thenCompose( x -> Try.success( x + 1 ) );
 *
 *      assertThat( result.get() ).isEqualTo( 4 );
 *      assertThat( result.isSuccess() ).isTrue();
 * </pre>
 *
 * @param <T> the type of the parameter encapsulated by the Try.
 */
public interface Try<T> {
    static <U, V> Function<U, Try<V>> of(FaillibleFunction<? super U, ? extends V, Exception> function) {
        Objects.requireNonNull(function, "Function should not be null");
        return x -> {
            try {
                return success(function.apply(x));
            } catch (Exception e) {
                return failure(e);
            }
        };
    }

    static <U> Try<U> run(FaillibleSupplier<? extends U, Exception> supplier) {
        Objects.requireNonNull(supplier, "Supplier should not be null");
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    static Try<Void> run(FaillibleRunnable<Exception> runnable) {
        Objects.requireNonNull(runnable, "Runnable should not be null");
        try {
            runnable.apply();
            return success(null);
        } catch (Exception e) {
            return failure(e);
        }
    }

    static <U, V> Function<U, Try<V>> of(
            FaillibleFunction<? super U, ? extends V, Exception> function, RetryConfig retryConfig) {
        var retryableFunction = new RetryableFunction<>(function, retryConfig);
        return of(retryableFunction);
    }

    static <U> Try<U> run(FaillibleSupplier<? extends U, Exception> supplier, RetryConfig retryConfig) {
        var retryableSupplier = new RetryableSupplier<>(supplier, retryConfig);
        return run(retryableSupplier);
    }

    static Try<Void> run(FaillibleRunnable<Exception> runnable, RetryConfig retryConfig) {
        var retryableRunnable = new RetryableRunnable(runnable, retryConfig);
        return run(retryableRunnable);
    }

    static <U> Try<U> failure(Exception exception) {
        return new Failure<>(exception);
    }

    static <U> Try<U> success(U value) {
        return new Success<>(value);
    }

    <R> Try<R> thenCompose(FaillibleFunction<? super T, Try<? extends R>, Exception> function);

    Try<Void> thenRun(FaillibleRunnable<Exception> runnable);

    Try<Void> thenAccept(FaillibleConsumer<? super T, Exception> consumer);

    default <R> Try<R> thenApply(FaillibleFunction<? super T, ? extends R, Exception> function) {
        var tryFunction = of(function);
        return thenCompose(tryFunction::apply);
    }

    default <R> Try<R> thenCompose(
            FaillibleFunction<? super T, Try<? extends R>, Exception> function, RetryConfig retryConfig) {
        var retryableFunction = new RetryableFunction<>(function, retryConfig);
        return thenCompose(retryableFunction);
    }

    default <R> Try<R> thenApply(
            FaillibleFunction<? super T, ? extends R, Exception> function, RetryConfig retryConfig) {
        var retryableFunction = new RetryableFunction<>(function, retryConfig);
        return thenApply(retryableFunction);
    }

    default Try<Void> thenRun(FaillibleRunnable<Exception> runnable, RetryConfig retryConfig) {
        RetryableRunnable retryableRunnable = new RetryableRunnable(runnable, retryConfig);
        return thenRun(retryableRunnable);
    }

    default Try<Void> thenAccept(FaillibleConsumer<? super T, Exception> consumer, RetryConfig retryConfig) {
        var retryableConsumer = new RetryableConsumer<>(consumer, retryConfig);
        return thenAccept(retryableConsumer);
    }

    T get();

    T getOrElse(T defaultValue);

    T getOrElse(Supplier<T> defaultValueSupplier);

    <R> Try<R> orElseRecover(FaillibleFunction<? super Exception, ? extends R, Exception> function);

    static <U, V, R> Try<R> combine(
            Try<U> try1, Try<V> try2, FaillibleBiFunction<? super U, ? super V, ? extends R, Exception> function) {
        return try1.thenCompose(boundValue1 -> try2.thenApply(boundValue2 -> function.apply(boundValue1, boundValue2)));
    }

    boolean isSuccess();
}
