package org.orel;

import org.orel.monad.Try;

import java.util.function.Supplier;

public class Main {

    public static void main(String[] args) {

        // This is an example

        Supplier<Long> delay = () -> 10L;

        RetryConfig retryConfig = new RetryConfig( delay, 5 );

        Try<Integer> result = Try.run( () -> 2 )
                .thenApply( x -> x + 1, retryConfig )
                .thenCompose( x -> Try.success( x + 1 ) );

    }
}