package org.orel.operator;

@FunctionalInterface
public interface FaillibleConsumer<T, E extends Throwable> {

    void accept(T t) throws E;
}
