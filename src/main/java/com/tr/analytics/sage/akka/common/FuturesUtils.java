package com.tr.analytics.sage.akka.common;

import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.Recover;
import akka.pattern.Patterns;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;


import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class FuturesUtils {
    static public <T> Future<T> futureWithTimeout(Future<T> future, FiniteDuration duration, ExecutionContext executionContext, Scheduler scheduler) {
        Exception exception = new TimeoutException();
        Future<T> failedFuture = Futures.failed(exception);
        Future<T> delayed = Patterns.after(
                duration, scheduler, executionContext, failedFuture
        );

        return Futures.firstCompletedOf(Arrays.<Future<T>>asList(future, delayed), executionContext);
    }




    static public <T,E extends Exception> Future<T> futureWithTimeout(Future<T> future, FiniteDuration duration, final Function_WithExceptions<Throwable,T, E> recoverFun, ExecutionContext executionContext, Scheduler scheduler) {
        Exception exception = new TimeoutException();
        Future<T> failedFuture = Futures.failed(exception);
        Future<T> delayed = Patterns.after(
                duration, scheduler, executionContext, failedFuture
        );

        return Futures.firstCompletedOf(Arrays.<Future<T>>asList(future, delayed), executionContext).recover(new Recover<T>() {
            public T recover(Throwable e) throws java.lang.Throwable {
                return recoverFun.apply(e);
            }
        }, executionContext);
    }

    static public <T,E extends Exception> Future<T> futureWithTimeout(Future<T> future, FiniteDuration duration, final Function_WithExceptions<Throwable,T,E> recover, ActorSystem system)
    {
        return futureWithTimeout(future, duration, recover, system.dispatcher(), system.scheduler());
    }

    static public <T,R> Mapper toMapper(final Function<T, R> mapper) {
        return new Mapper<T, R>() {
            public R apply(T a) {
                return mapper.apply(a);
            }};
    }

}
