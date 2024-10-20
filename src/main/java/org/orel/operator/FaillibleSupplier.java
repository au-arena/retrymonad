package org.orel.operator;

@FunctionalInterface
public interface FaillibleSupplier<T, E extends Exception> {

    T get() throws E;

}
