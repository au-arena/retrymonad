package org.orel.operator;

@FunctionalInterface
public interface FaillibleFunction<T, R, E extends Exception> {

    R apply(T t) throws E;
}
