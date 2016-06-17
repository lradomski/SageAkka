package com.tr.analytics.sage.akka.common;

@FunctionalInterface
public interface Function2_WithExceptions<T, T2, R, E extends Throwable> {
    R apply(T t, T2 t2) throws E;
}
