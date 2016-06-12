package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.Kill;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.tr.analytics.sage.akka.data.*;
import com.tr.analytics.sage.api.Trade;
import com.tr.analytics.sage.shard.engine.TradeFactory;
import scala.concurrent.duration.Duration;

import java.sql.Time;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class CalcRicTest extends ActorTestCaseBase {

    protected void setUp() throws Exception {
        super.setUp();

    }

    public void tearDown() throws Exception {
        super.tearDown();

    }

    final int quoteId = 1;
    final String ric = Integer.toString(quoteId);
    final StartCalcSingleRic req = new StartCalcSingleRic("test", "test", 1, ric);

    public void test_WaitForResp_Timeout() {
        new JavaTestKit(system) {
            {

                final JavaTestKit calcShard = new JavaTestKit(system);

                // outer JavaTestKit simulates RicStore
                final TestActorRef<CalcRic> calcRic = TestActorRef.create(system, Props.create(CalcRic.class, calcShard.getRef(), req, getRef(), system.dispatcher()), ric);

                watch(calcRic);
                expectTerminated(CalcRic.INIT_TIMEOUT.plus(Duration.create(300, TimeUnit.MILLISECONDS)), calcRic);
            }};
    }


    public void test_WaitForResp_CalcShardTerminate() {
        new JavaTestKit(system) {
            {

                final JavaTestKit calcShard = new JavaTestKit(system);

                // outer JavaTestKit simulates RicStore
                final TestActorRef<CalcRic> calcRic = TestActorRef.create(system, Props.create(CalcRic.class, calcShard.getRef(), req, getRef(), system.dispatcher()), ric);

                watch(calcRic);
                system.stop(calcShard.getRef());
                expectTerminated(Duration.create(300, TimeUnit.MILLISECONDS), calcRic);
            }};
    }

    public void test_WaitForResp_RicStoreTerminate() {
        new JavaTestKit(system) {
            {

                final JavaTestKit ricStore = new JavaTestKit(system);

                // outer JavaTestKit simulates calcShard
                final TestActorRef<CalcRic> calcRic = TestActorRef.create(system, Props.create(CalcRic.class, getRef(), req, ricStore.getRef(), system.dispatcher()), ric);

                watch(calcRic);
                system.stop(ricStore.getRef());
                expectTerminated(Duration.create(300, TimeUnit.MILLISECONDS), calcRic);
            }};
    }

    public void test_WaitForResp_Response() {
        new JavaTestKit(system) {
            {

                final JavaTestKit calcShard = new JavaTestKit(system);

                // outer JavaTestKit simulates RicStore
                final TestActorRef<CalcRic> calcRic = TestActorRef.create(system, Props.create(CalcRic.class, calcShard.getRef(), req, getRef(), system.dispatcher()), ric);

            }};
    }

}