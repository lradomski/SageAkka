package com.tr.analytics.sage.akka;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import junit.framework.TestCase;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class ActorTestCaseBase extends TestCase {

    protected ActorSystem system;

    @Override
    protected void setUp() throws Exception {
        system = ActorSystem.create("Test");
    }

    final FiniteDuration EXEPECT_TO = Duration.create(300, TimeUnit.MILLISECONDS);

    @Override
    protected void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
        Future f = system.whenTerminated();
        Await.result(f, Duration.create(1, TimeUnit.SECONDS));
    }

    public void testBase()
    {}
}
