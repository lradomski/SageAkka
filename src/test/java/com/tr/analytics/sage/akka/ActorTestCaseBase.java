package com.tr.analytics.sage.akka;

import akka.actor.ActorSystem;
import junit.framework.TestCase;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class ActorTestCaseBase extends TestCase {

    protected ActorSystem system;

    @Override
    protected void setUp() throws Exception {
        system = ActorSystem.create("Test");
    }

    @Override
    protected void tearDown() throws Exception {
        system.terminate();
        Future f = system.whenTerminated();
        Await.result(f, Duration.create(1, TimeUnit.SECONDS));
    }
}
