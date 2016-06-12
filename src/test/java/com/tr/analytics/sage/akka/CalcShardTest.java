package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.tr.analytics.sage.akka.data.*;
import com.tr.analytics.sage.api.Trade;
import com.tr.analytics.sage.shard.engine.TradeFactory;
import common.TestManualDispatcher;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class CalcShardTest extends ActorTestCaseBase {

    protected void setUp() throws Exception {
        super.setUp();

    }

    public void tearDown() throws Exception {
        testDisp.clear();
        super.tearDown();

    }

    final int quoteId = 1;
    final String ric = Integer.toString(quoteId);

    final int quoteId2 = 2;
    final String ric2 = Integer.toString(quoteId2);

    final int quoteId3 = 3;
    final String ric3 = Integer.toString(quoteId3);


    final StartCalcMultiRic req = new StartCalcMultiRic("test", "test", 1, Arrays.asList(ric, ric2, ric3));
    final String name = req.toActorName(0);
    final TestManualDispatcher testDisp = new TestManualDispatcher();
    final FiniteDuration EXEPECT_TO = Duration.create(300, TimeUnit.MILLISECONDS);



    public void test_WaitForResp_Timeout() {
        new JavaTestKit(system) {
            {

                final JavaTestKit calcAsm = new JavaTestKit(system);

                // outer JavaTestKit simulates RicStore
                final TestActorRef<CalcShard> calcShard = TestActorRef.create(system, Props.create(CalcShard.class, req, calcAsm.getRef(), system.dispatcher()), name);

                watch(calcShard);
                expectTerminated(CalcShard.INIT_TIMEOUT.plus(Duration.create(300, TimeUnit.MILLISECONDS)), calcShard);
            }};
    }


    public void test_WaitForResp_CalcAsmTerminate() {
        new JavaTestKit(system) {
            {

                final JavaTestKit calcAsm = new JavaTestKit(system);

                // outer JavaTestKit simulates RicStore
                final TestActorRef<CalcShard> calcShard = TestActorRef.create(system, Props.create(CalcShard.class, req, calcAsm.getRef(), system.dispatcher()), name);

                watch(calcShard);
                system.stop(calcAsm.getRef());
                expectTerminated(Duration.create(300, TimeUnit.MILLISECONDS), calcShard);
            }
        };
    }


}