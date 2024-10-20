package org.orel.monad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.orel.RetryConfig;
import org.orel.operator.FaillibleBiFunction;
import org.orel.operator.FaillibleConsumer;
import org.orel.operator.FaillibleFunction;
import org.orel.operator.FaillibleRunnable;
import org.orel.operator.FaillibleSupplier;

import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class TryTest {
    private static final Supplier<Long> DELAY = () -> 10L;

    static FaillibleFunction<Integer, Integer, Exception> FUNCTION = x -> x + 1;
    static FaillibleBiFunction<Integer, Integer, Integer, Exception> BIFUNCTION = Integer::sum;
    static FaillibleSupplier<Integer, Exception> SUPPLIER = () -> 5;
    static FaillibleRunnable<Exception> RUNNABLE = () -> {
        /** do something **/
    };
    static FaillibleConsumer<Integer, Exception> CONSUMER = x -> {
        /** do something **/
    };

    static FaillibleFunction<Integer, Integer, Exception> THROWING_FUNCTION = x -> {
        throw new Exception("failed");
    };
    static FaillibleBiFunction<Integer, Integer, Integer, Exception> THROWING_BIFUNCTION = (x, y) -> {
        throw new Exception("failed");
    };
    static FaillibleSupplier<Integer, Exception> THROWING_SUPPLIER = () -> {
        throw new Exception("failed");
    };
    static FaillibleRunnable<Exception> THROWING_RUNNABLE = () -> {
        throw new Exception("failed");
    };
    static FaillibleConsumer<Integer, Exception> THROWING_CONSUMER = x -> {
        throw new Exception("failed");
    };

    static Stream<Try<?>> successfulTry() {
        return Stream.of(
                Try.of(FUNCTION).apply(1),
                Try.run(SUPPLIER),
                Try.run(RUNNABLE),
                Try.run(SUPPLIER).thenApply(FUNCTION),
                Try.run(SUPPLIER).thenCompose(x -> Try.success(3)),
                Try.run(SUPPLIER).thenRun(RUNNABLE),
                Try.run(SUPPLIER).thenAccept(CONSUMER),
                Try.run(THROWING_SUPPLIER).orElseRecover(e -> 2),
                Try.run(THROWING_RUNNABLE).orElseRecover(e -> 2),
                Try.of(THROWING_FUNCTION).apply(1).orElseRecover(e -> 2),
                Try.combine(Try.success(2), Try.success(3), BIFUNCTION));
    }

    static Stream<Try<?>> failedTry() {
        return Stream.of(
                Try.of(THROWING_FUNCTION).apply(1),
                Try.run(THROWING_SUPPLIER),
                Try.run(THROWING_RUNNABLE),
                Try.run(RUNNABLE).thenApply(x -> {
                    throw new Exception();
                }),
                Try.run(RUNNABLE).thenCompose(x -> {
                    throw new Exception();
                }),
                Try.run(RUNNABLE).thenRun(THROWING_RUNNABLE),
                Try.run(RUNNABLE).thenAccept(x -> {
                    throw new Exception();
                }),
                Try.run(SUPPLIER).thenApply(THROWING_FUNCTION),
                Try.run(SUPPLIER).thenCompose(x -> {
                    throw new Exception();
                }),
                Try.run(SUPPLIER).thenRun(THROWING_RUNNABLE),
                Try.run(SUPPLIER).thenAccept(THROWING_CONSUMER),
                Try.combine(Try.failure(new Exception()), Try.success(3), BIFUNCTION),
                Try.combine(Try.success(3), Try.failure(new Exception()), BIFUNCTION),
                Try.combine(Try.success(3), Try.success(3), THROWING_BIFUNCTION));
    }

    @ParameterizedTest
    @MethodSource("successfulTry")
    void shouldBeSuccess(Try<Integer> successfulTry) throws Exception {
        assertThat(successfulTry.isSuccess()).isTrue();
        assertDoesNotThrow(successfulTry::get);
        assertThat(successfulTry).isInstanceOf(Success.class);

        if (successfulTry.get() != null) {
            assertThat(successfulTry.thenApply(FUNCTION)).isEqualTo(Try.success(FUNCTION.apply(successfulTry.get())));
            assertThat(successfulTry.thenCompose(x -> Try.success(x + 1)))
                    .isEqualTo(Try.success(successfulTry.get() + 1));
        }

        FaillibleRunnable<Exception> runnable = mock(FaillibleRunnable.class);
        FaillibleConsumer<Integer, Exception> consumer = mock(FaillibleConsumer.class);

        var thenRunRunnable = successfulTry.thenRun(runnable);
        var thenAcceptConsumer = successfulTry.thenAccept(consumer);

        assertThat(thenRunRunnable.get()).isNull();
        assertThat(thenRunRunnable.isSuccess()).isTrue();

        assertThat(thenAcceptConsumer.get()).isNull();
        assertThat(thenAcceptConsumer.isSuccess()).isTrue();

        verify(runnable, times(1)).apply();
        verify(consumer, times(1)).accept(successfulTry.get());

        assertThat(successfulTry.getOrElse(60)).isEqualTo(successfulTry.get());
        assertThat(successfulTry.getOrElse(() -> 60)).isEqualTo(successfulTry.get());
        assertThat(successfulTry.orElseRecover(e -> 60)).isEqualTo(successfulTry);
    }

    @ParameterizedTest
    @MethodSource("failedTry")
    void shouldBeFailure(Try<Integer> failedTry) {
        assertThat(failedTry.isSuccess()).isFalse();
        assertThrows(NoSuchElementException.class, failedTry::get);
        assertThat(failedTry).isInstanceOf(Failure.class);
        assertThat(((Failure<Integer>) failedTry).exception()).isNotNull();

        FaillibleRunnable<Exception> runnable = mock(FaillibleRunnable.class);
        FaillibleConsumer<Integer, Exception> consumer = mock(FaillibleConsumer.class);
        FaillibleFunction<Integer, Integer, Exception> function = mock(FaillibleFunction.class);

        assertThat(failedTry.thenRun(runnable)).isEqualTo(failedTry);
        assertThat(failedTry.thenAccept(consumer)).isEqualTo(failedTry);

        assertThat(failedTry.thenApply(function)).isEqualTo(failedTry);
        assertThat(failedTry.thenCompose(x -> Try.success(""))).isEqualTo(failedTry);

        verifyNoInteractions(runnable);
        verifyNoInteractions(consumer);
        verifyNoInteractions(function);

        assertThat(failedTry.getOrElse(60)).isEqualTo(60);
        assertThat(failedTry.getOrElse(() -> 60)).isEqualTo(60);
        assertThat(failedTry.orElseRecover(e -> 60)).isEqualTo(Try.success(60));
    }

    @Test
    void chainedTryShouldSucceed() throws Exception {
        // given
        FaillibleConsumer<Integer, Exception> consumer = mock(FaillibleConsumer.class);

        // when
        var result = Try.run(() -> 2)
                .thenApply(x -> x + 1)
                .thenCompose(x -> Try.success(x + 1))
                .thenAccept(consumer)
                .get();

        // then
        assertThat(result).isNull();
        verify(consumer, times(1)).accept(4);
    }

    @Test
    void tryShouldHaveTheCorrectValue() throws Exception {
        // given
        var value = 6;
        var tryFunction = Try.of(FUNCTION).apply(value);
        var trySupplier = Try.run(SUPPLIER);

        // when / then
        assertThat(tryFunction.get()).isEqualTo(FUNCTION.apply(value));
        assertThat(trySupplier.get()).isEqualTo(SUPPLIER.get());
    }

    static Stream<BiFunction<FaillibleRunnable<Exception>, RetryConfig, Try<Void>>> tryAcceptingFaillibleRunnable() {
        return Stream.of(Try::run, Try.run(SUPPLIER)::thenRun);
    }

    @ParameterizedTest
    @MethodSource("tryAcceptingFaillibleRunnable")
    void tryRunningFailingrunnableShouldRetryThenFail(
            BiFunction<FaillibleRunnable<Exception>, RetryConfig, Try<Void>> tryRunner) throws Exception {
        // given
        int retries = 3;
        var RetryConfig = new RetryConfig(DELAY, retries);
        FaillibleRunnable<Exception> runnable = mock(FaillibleRunnable.class);
        doThrow(new Exception()).when(runnable).apply();

        // when
        var result = tryRunner.apply(runnable, RetryConfig);

        // then
        verify(runnable, times(retries)).apply();
        assertThat(result.isSuccess()).isFalse();
    }

    static Stream<BiFunction<FaillibleFunction<Integer, Integer, Exception>, RetryConfig, Try<Integer>>>
            tryAcceptingFaillibleFunction() {
        return Stream.of(
                (function, RetryConfig) -> Try.of(function, RetryConfig).apply(2), Try.run(SUPPLIER)::thenApply);
    }

    @ParameterizedTest
    @MethodSource("tryAcceptingFaillibleFunction")
    void tryRunningFailingFunctionShouldRetryThenFail(
            BiFunction<FaillibleFunction<Integer, Integer, Exception>, RetryConfig, Try<Integer>> tryRunner)
            throws Exception {
        // given
        int retries = 3;
        var RetryConfig = new RetryConfig(DELAY, retries);
        FaillibleFunction<Integer, Integer, Exception> function = mock(FaillibleFunction.class);
        doThrow(new Exception()).when(function).apply(any());

        // when
        var result = tryRunner.apply(function, RetryConfig);

        // then
        verify(function, times(retries)).apply(any());
        shouldBeFailure(result);
    }

    @Test
    void shouldRetrySupplier() throws Exception {
        // given
        int retries = 3;
        var RetryConfig = new RetryConfig(DELAY, retries);
        var supplier = mock(FaillibleSupplier.class);
        doThrow(new Exception()).when(supplier).get();

        // when
        Try.run(supplier, RetryConfig);

        // then
        verify(supplier, times(retries)).get();
    }

    @Test
    void tryShouldEventuallySucceed() throws Exception {
        // given
        int retries = 5;
        int value = 7;
        var RetryConfig = new RetryConfig(DELAY, retries);
        FaillibleSupplier<Integer, Exception> supplier = mock(FaillibleSupplier.class);
        doThrow(new Exception(), new Exception()).doReturn(value).when(supplier).get();

        // when
        var result = Try.run(supplier, RetryConfig);

        // then
        verify(supplier, times(3)).get();
        assertThat(result.get()).isEqualTo(value);
        shouldBeSuccess(result);
    }

    @Test
    void getShouldThrowAfterFailRetry() throws Exception {
        // given
        var RetryConfig = new RetryConfig(DELAY, 2);
        FaillibleSupplier<Integer, Exception> supplier = mock(FaillibleSupplier.class);
        var exceptionToThrow = new Exception("The error");
        doThrow(exceptionToThrow).when(supplier).get();

        // when
        var result = Try.run(supplier, RetryConfig);
        var thrownException = assertThrows(NoSuchElementException.class, result::get);

        // then
        assertThat(thrownException.getCause()).isInstanceOf(RetryConfig.MaxRetriesException.class);
        assertThat(thrownException.getCause().getCause()).isEqualTo(exceptionToThrow);
        shouldBeFailure(result);
    }

    @Test
    void getShouldThrow() throws Exception {
        // given
        FaillibleSupplier<Integer, Exception> supplier = mock(FaillibleSupplier.class);
        var exceptionToThrow = new Exception("The error");
        doThrow(exceptionToThrow).when(supplier).get();

        // when
        var result = Try.run(supplier);
        var thrownException = assertThrows(NoSuchElementException.class, result::get);

        // then
        assertThat(thrownException.getCause()).isEqualTo(exceptionToThrow);
        shouldBeFailure(result);
    }

    @Test
    void combineShouldReturnCorrectResult() throws Exception {
        // given
        int value1 = 2;
        int value2 = 3;
        var try1 = Try.success(value1);
        var try2 = Try.success(value2);

        // when
        var result = Try.combine(try1, try2, Integer::sum);

        // then
        shouldBeSuccess(result);
        assertThat(result.get()).isEqualTo(value1 + value2);
    }
}
