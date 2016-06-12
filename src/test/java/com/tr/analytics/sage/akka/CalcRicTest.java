package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.tr.analytics.sage.akka.data.CalcResult;
import com.tr.analytics.sage.akka.data.CalcUpdate;
import com.tr.analytics.sage.akka.data.StartCalcSingleRic;
import com.tr.analytics.sage.akka.data.TradeTotals;
import com.tr.analytics.sage.api.Trade;
import com.tr.analytics.sage.shard.engine.TradeFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CalcRicTest extends ActorTestCaseBase {

    protected void setUp() throws Exception {
        super.setUp();

    }

    public void tearDown() throws Exception {
        testDisp.clear();
        super.tearDown();

    }

    final int quoteId = 1;
    final String ric = Integer.toString(quoteId);
    final StartCalcSingleRic req = new StartCalcSingleRic("test", "test", 1, ric);
    final ManualDispatcher testDisp = new ManualDispatcher();

    static class ManualDispatcher implements ExecutionContext
    {
        final Queue<Runnable> runnables = new LinkedList<>();
        //final ExecutorService es = Executors.newSingleThreadExecutor();

        @Override
        public void execute(Runnable runnable)
        {
            runnables.add(runnable);
        }

        public boolean allowOne()
        {
            if (0 < runnables.size()) {
                //es.submit(runnables.remove());
                runnables.remove().run();
                return true;
            }
            else return false;
        }


        public void allowAll()
        {
            while (allowOne())
            {
            }
        }

        public void clear()
        {
            runnables.clear();
        }

        public Queue<Runnable> getQueue()
        {
            return runnables;
        }

        @Override
        public void reportFailure(Throwable cause) {

        }

        @Override
        public ExecutionContext prepare() {
            return this;
        }
    }


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
            }
        };
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
            }
        };
    }

    public void testSimpleFlow() {
        new JavaTestKit(system) {
            {

                final JavaTestKit calcShard = new JavaTestKit(system);
                final ActorRef ricStore = getRef();

                // outer JavaTestKit simulates RicStore
                final TestActorRef<CalcRic> calcRic = TestActorRef.create(system, Props.create(CalcRic.class, calcShard.getRef(), req, getRef(), testDisp), ric);

                Trade t = TradeFactory.simple(quoteId, 10, 100);
                Trade t2 = TradeFactory.simple(quoteId, 20, 200);
                Trade[] trades = new Trade[] { t, t2 };
                TradeTotals tt = TradeTotals.from(new RicStore.Trades(trades, trades.length));

                Trade t3 = TradeFactory.simple(quoteId, 30, 300);
                Trade t4 = TradeFactory.simple(quoteId, 40, 400);
                Trade t5 = TradeFactory.simple(quoteId, 50, 500);
                Trade[] trades2 = new Trade[] { t, t2, t3, t4, t5 };
                TradeTotals tt2 = TradeTotals.from(new RicStore.Trades(trades2, trades.length));

                assertTrue(calcRic.underlyingActor().stateData().totals.equals(new TradeTotals()));
                assertTrue(calcRic.underlyingActor().stateName() == CalcRic.States.WaitForResp);


                // STEP
                calcRic.tell(new CalcResult<RicStore.Trades>(req.getId(), new RicStore.Trades(trades, trades.length)), ricStore);
                calcShard.expectNoMsg(Duration.create(300, TimeUnit.MILLISECONDS)); // testDisp is not allowing execution of response calculation

                calcRic.tell(new CalcUpdate<Trade>(req.getId(), t3), ricStore);
                calcShard.expectNoMsg(Duration.create(300, TimeUnit.MILLISECONDS)); // testDisp is not allowing execution of response calculation

                calcRic.tell(new CalcUpdate<Trade>(req.getId(), t4), ricStore);
                calcShard.expectNoMsg(Duration.create(300, TimeUnit.MILLISECONDS)); // testDisp is not allowing execution of response calculation

                // STEP
                testDisp.allowOne(); // now let that response calculation through - we should get response and all the updates
                calcShard.expectMsgEquals(Duration.create(300, TimeUnit.MILLISECONDS), new CalcResult<>(req.getId(), tt));
                calcShard.expectMsgEquals(Duration.create(300, TimeUnit.MILLISECONDS), new CalcUpdate<>(req.getId(), TradeTotals.from(t3)));
                calcShard.expectMsgEquals(Duration.create(300, TimeUnit.MILLISECONDS), new CalcUpdate<>(req.getId(), TradeTotals.from(t4)));

                // STEP now force reset of the calc with new response
                calcRic.tell(new CalcResult<RicStore.Trades>(req.getId(), new RicStore.Trades(new Trade[] {}, 0)), ricStore);
                calcShard.expectNoMsg(Duration.create(300, TimeUnit.MILLISECONDS)); // testDisp is not allowing execution of response calculation

                // STEP now we have a new update - while waiting for calculation of response (refresh)
                calcRic.tell(new CalcUpdate<Trade>(req.getId(), t5), ricStore);
                calcShard.expectMsgEquals(new CalcUpdate<>(req.getId(), TradeTotals.from(t5)));

                testDisp.allowOne(); // now let that refresh response calculation through - we should get refresh (new) and t5
                calcShard.expectMsgEquals(new CalcResult<>(req.getId(), new TradeTotals())); // we zero-ed out on refresh
                calcShard.expectMsgEquals(Duration.create(300, TimeUnit.MILLISECONDS), new CalcUpdate<>(req.getId(), TradeTotals.from(t5)));

//                new AwaitCond(
//                        duration("1 second"),  // maximum wait time
//                        duration("100 millis") // interval at which to check the condition
//                ) {
//                    protected boolean cond() {
//                        // typically used to wait for something to start up
//                        return calcRic.underlyingActor().stateName() == CalcRic.States.SendCalc;
//                    }
//                };
            }};
    }

}