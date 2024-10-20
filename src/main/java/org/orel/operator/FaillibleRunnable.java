package org.orel.operator;

@FunctionalInterface
public interface FaillibleRunnable<E extends Exception> {

    void apply() throws Exception;

}
