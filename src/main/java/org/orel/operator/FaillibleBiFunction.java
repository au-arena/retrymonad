package org.orel.operator;

public interface FaillibleBiFunction<U, V, R, E extends Exception> {
    R apply(U arg1, V arg2) throws E;
}