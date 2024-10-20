package org.orel.operator;

import org.junit.jupiter.api.Test;
import org.orel.RetryConfig;
import org.orel.retryable.RetryableSupplierWithValidation;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class RetryableSupplierWithValidationTest {

    private static final Supplier<Long> DELAY = () -> 10L;

    @Test
    void shouldSucceedWhenValidatorEventuallyTrue() throws Exception {
        // given
        int retries = 3;
        var retryConfig = new RetryConfig(DELAY, retries);
        Supplier<Integer> supplier = mock(Supplier.class);
        doReturn(4, 5, 6).when(supplier).get();

        Predicate<Integer> validator = x -> x > 4;

        var retryableSupplier = new RetryableSupplierWithValidation<>(supplier::get, retryConfig, validator);

        // when
        int result = retryableSupplier.get();

        // then
        assertThat(result).isEqualTo(5);
        verify(supplier, times(2)).get();
    }

    @Test
    void shouldSucceedWhenValidatorAlwaysTrue() throws Exception {
        // given
        int retries = 3;
        var retryConfig = new RetryConfig(DELAY, retries);
        Supplier<Integer> supplier = mock(Supplier.class);
        doReturn(4, 5, 6).when(supplier).get();

        var retryableSupplier = new RetryableSupplierWithValidation<>(supplier::get, retryConfig, x -> true);

        // when
        int result = retryableSupplier.get();

        // then
        assertThat(result).isEqualTo(4);
        verify(supplier, times(1)).get();
    }

    @Test
    void shouldFailWhenValidatorAlwaysFalse() {
        // given
        int retries = 3;
        var retryConfig = new RetryConfig(DELAY, retries);
        Supplier<Integer> supplier = mock(Supplier.class);
        doReturn(4, 5, 6).when(supplier).get();

        var retryableSupplier = new RetryableSupplierWithValidation<>(supplier::get, retryConfig, x -> false);

        // when
        Exception exception = assertThrows(Exception.class, retryableSupplier::get);

        // then
        verify(supplier, times(retries)).get();
        assertThat(exception).isInstanceOf(RetryConfig.MaxRetriesException.class);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowTheCauseException() throws Exception {
        // given
        int retries = 3;
        var retryConfig = new RetryConfig(DELAY, retries);
        FaillibleSupplier<Integer, Exception> supplier = mock(FaillibleSupplier.class);
        var exceptionToThrow = new Exception("failed");
        doThrow(exceptionToThrow).when(supplier).get();

        var retryableSupplier = new RetryableSupplierWithValidation<>(supplier, retryConfig, x -> false);

        // when
        Exception thrownException = assertThrows(Exception.class, retryableSupplier::get);

        // then
        verify(supplier, times(retries)).get();
        assertThat(thrownException).isInstanceOf(RetryConfig.MaxRetriesException.class);
        assertThat(thrownException.getCause()).isEqualTo(exceptionToThrow);
    }

    @Test
    void nonPositiveRetryNumberRetriesUntilSuccess() throws Exception {
        // given
        var retryConfig = new RetryConfig(DELAY, 0);
        Supplier<Integer> supplier = mock(Supplier.class);
        doReturn(4, 5, 6).when(supplier).get();

        Predicate<Integer> validator = mock(Predicate.class);
        doReturn(false, false, true).when(validator).test(any());

        var retryableSupplier = new RetryableSupplierWithValidation<>(supplier::get, retryConfig, validator);

        // when
        int result = retryableSupplier.get();

        // then
        assertThat(result).isEqualTo(6);
        verify(supplier, times(3)).get();
    }


}
