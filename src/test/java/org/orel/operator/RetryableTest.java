package org.orel.operator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orel.RetryConfig;
import org.orel.retryable.RetryableConsumer;
import org.orel.retryable.RetryableFunction;
import org.orel.retryable.RetryableRunnable;
import org.orel.retryable.RetryableSupplier;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RetryableTest {

    private static final Supplier<Long> DELAY = () -> 10L;

    @Test
    void runnableShouldRetryAndFail() throws Exception {
        // given
        int retries = 3;
        var retryConfig = new RetryConfig(DELAY, retries);
        FaillibleRunnable<Exception> runnable = mock(FaillibleRunnable.class);
        var exceptionToThrow = new Exception("failed");
        doThrow(exceptionToThrow).when(runnable).apply();

        var retryableRunnable = new RetryableRunnable(runnable, retryConfig);

        // when
        var thrownException = assertThrows(Exception.class, retryableRunnable::apply);

        // then
        verify(runnable, times(retries)).apply();
        assertThat(thrownException).isInstanceOf(RetryConfig.MaxRetriesException.class);
        assertThat(thrownException.getCause()).isEqualTo(exceptionToThrow);
    }

    @Test
    void consumerShouldRetryAndFail() throws Exception {
        // given
        int retries = 3;
        int value = 2;
        var RetryConfig = new RetryConfig(DELAY, retries);
        FaillibleConsumer<Integer, Exception> consumer = mock(FaillibleConsumer.class);
        var exceptionToThrow = new Exception("failed");
        doThrow(exceptionToThrow).when(consumer).accept(value);

        var retryableConsumer = new RetryableConsumer<>(consumer, RetryConfig);

        // when
        var thrownException = assertThrows(Exception.class, () -> retryableConsumer.accept(value));

        // then
        verify(consumer, times(retries)).accept(value);
        assertThat(thrownException).isInstanceOf(RetryConfig.MaxRetriesException.class);
        assertThat(thrownException.getCause()).isEqualTo(exceptionToThrow);
    }

    @Test
    void supplierShouldRetryAndFail() throws Exception {
        // given
        int retries = 3;
        var RetryConfig = new RetryConfig(DELAY, retries);
        FaillibleSupplier<Integer, Exception> supplier = mock(FaillibleSupplier.class);
        var exceptionToThrow = new Exception("failed");
        doThrow(exceptionToThrow).when(supplier).get();

        var retryableSupplier = new RetryableSupplier<>(supplier, RetryConfig);

        // when
        var thrownException = assertThrows(Exception.class, retryableSupplier::get);

        // then
        verify(supplier, times(retries)).get();
        assertThat(thrownException).isInstanceOf(RetryConfig.MaxRetriesException.class);
        assertThat(thrownException.getCause()).isEqualTo(exceptionToThrow);
    }

    @Test
    void functionShouldRetryAndFail() throws Exception {
        // given
        int retries = 3;
        int value = 2;
        var RetryConfig = new RetryConfig(DELAY, retries);

        FaillibleFunction<Integer, Integer, Exception> function = mock(FaillibleFunction.class);
        var exceptionToThrow = new Exception("failed");
        doThrow(exceptionToThrow).when(function).apply(value);

        // when
        var thrownException =
                assertThrows(Exception.class, () -> new RetryableFunction<>(function, RetryConfig).apply(value));

        // then
        verify(function, times(retries)).apply(value);
        assertThat(thrownException).isInstanceOf(RetryConfig.MaxRetriesException.class);
        assertThat(thrownException.getCause()).isEqualTo(exceptionToThrow);
    }

    @Test
    void functionShouldReturnCorrectResult() throws Exception {
        // given
        int retries = 3;
        var RetryConfig = new RetryConfig(DELAY, retries);

        Function<Integer, Integer> function = x -> x + 1;

        // when
        var result = new RetryableFunction<>(function::apply, RetryConfig).apply(1);

        // then
        assertThat(result).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 5})
    void functionShouldEventuallySucceed(int retries) throws Exception {
        // given
        int value = 2;
        var RetryConfig = new RetryConfig(DELAY, retries);

        FaillibleFunction<Integer, Integer, Exception> function = mock(FaillibleFunction.class);
        var exceptionToThrow = new Exception("failed");
        doThrow(exceptionToThrow, exceptionToThrow)
                .doReturn(4, 5, 6)
                .when(function)
                .apply(value);

        // when
        var result = new RetryableFunction<>(function, RetryConfig).apply(value);

        // then
        verify(function, times(3)).apply(value);
        assertThat(result).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1, 3})
    void functionShouldSucceedWithoutRetrying(int retries) throws Exception {
        // given
        int value = 2;
        var RetryConfig = new RetryConfig(DELAY, retries);

        FaillibleFunction<Integer, Integer, Exception> function = mock(FaillibleFunction.class);
        doReturn(4, 5, 6).when(function).apply(value);

        // when
        var result = new RetryableFunction<>(function, RetryConfig).apply(value);

        // then
        verify(function, times(1)).apply(value);
        assertThat(result).isEqualTo(4);
    }
}
