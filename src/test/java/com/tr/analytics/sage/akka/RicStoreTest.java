package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.tr.analytics.sage.akka.data.*;
import scala.concurrent.duration.Duration;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class RicStoreTest extends ActorTestCaseBase {

    protected void setUp() throws Exception {
        super.setUp();

    }

    public void tearDown() throws Exception {
        super.tearDown();

    }

    public void testPopulation() {
        new JavaTestKit(system) {
            {
                TestActorRef<RicStore> rs = TestActorRef.create(system, Props.create(RicStore.class, "1"), "1");

                // can also use JavaTestKit “from the outside”
                final JavaTestKit probe = new JavaTestKit(system);

                assertTrue(rs.underlyingActor().testGetTradeCount() == 0);
                assertTrue(rs.underlyingActor().testCountSubscribers() == 0);
                assertNull(rs.underlyingActor().testGetLastTrade());

                Trade t = TradeFactory.simple(1, 100.0, 1000);
                rs.tell(t, probe.getRef());
                expectNoMsg();
                assertTrue(rs.underlyingActor().testGetTradeCount() == 1);
                assertTrue(rs.underlyingActor().testCountSubscribers() == 0);
                assertTrue(rs.underlyingActor().testGetLastTrade() == t);

                Trade t2 = TradeFactory.simple(1, 200.0, 2000);
                rs.tell(t2, probe.getRef());
                expectNoMsg();
                assertTrue(rs.underlyingActor().testGetTradeCount() == 2);
                assertTrue(rs.underlyingActor().testCountSubscribers() == 0);
                assertTrue(rs.underlyingActor().testGetLastTrade() == t2);


                RicStore.Trades ts = rs.underlyingActor().testMakeTrades();
                assertTrue(ts.getCount() == 2);
                Iterator<Trade> it = ts.getTrades().iterator();
                assertEquals(t, it.next());
                assertEquals(t2, it.next());
                assertFalse(it.hasNext());
        }};
    }

    public void testInteraction() {
        TestActorRef<RicStore> rs = TestActorRef.create(system, Props.create(RicStore.class, "1"), TradeRouter.NAME);

        JavaTestKit tk = new JavaTestKit(system) {
            {
                // can also use JavaTestKit “from the outside”
                final JavaTestKit probe = new JavaTestKit(system);

                Trade t = TradeFactory.simple(1, 100.0, 1000);
                rs.tell(t, ActorRef.noSender());

                Trade t2 = TradeFactory.simple(1, 200.0, 2000);
                rs.tell(t2, ActorRef.noSender());

                StartCalc req = new StartCalc("test", "test", 0);
                rs.tell(req, getRef());

                new ExpectMsg<String>(Duration.create("1 second"), "Should get all trades") {
                    protected String match(Object m) {
                        if (m instanceof CalcResultCore)
                        {
                            CalcResult<RicStore.Trades> res = (CalcResult<RicStore.Trades>)m;
                            RicStore.Trades ts = res.getData();

                            assert(res.getId() == req.getId());

                            assertTrue(ts.getCount() == 2);
                            Iterator<Trade> it = ts.getTrades().iterator();
                            assertEquals(t, it.next());
                            assertEquals(t2, it.next());
                            assertFalse(it.hasNext());

                            return "ok";
                        } else {
                            throw noMatch();
                        }

                    }
                }.get();
                assertTrue(1 == rs.underlyingActor().testCountSubscribers());

                Trade t3 = TradeFactory.simple(1, 300.0, 3000);
                rs.tell(t3, ActorRef.noSender());
                expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), new CalcUpdate<Trade>(RicStore.UPDATE_ID, t3));

                final JavaTestKit sub2 = new JavaTestKit(system);
                StartCalc req2 = new StartCalc("test2", "test2", 1);
                rs.tell(req2, sub2.getRef());

                expectNoMsg();

                sub2.new ExpectMsg<String>(Duration.create("1 second"), "Should get all trades") {
                    protected String match(Object m) {
                        if (m instanceof CalcResultCore)
                        {
                            CalcResult<RicStore.Trades> res = (CalcResult<RicStore.Trades>)m;
                            RicStore.Trades ts = res.getData();

                            assert(res.getId() == req2.getId());

                            assertTrue(ts.getCount() == 3);
                            Iterator<Trade> it = ts.getTrades().iterator();
                            assertEquals(t, it.next());
                            assertEquals(t2, it.next());
                            assertEquals(t3, it.next());
                            assertFalse(it.hasNext());

                            return "ok";
                        } else {
                            throw noMatch();
                        }

                    }
                }.get();
                assertTrue(2 == rs.underlyingActor().testCountSubscribers());

                Trade t4 = TradeFactory.simple(1, 400.0, 4000);
                rs.tell(t4, ActorRef.noSender());
                expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), new CalcUpdate<Trade>(RicStore.UPDATE_ID, t4));
                sub2.expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), new CalcUpdate<Trade>(RicStore.UPDATE_ID, t4));


                watch(sub2.getRef());
                system.stop(sub2.getRef());
                expectTerminated(Duration.create(1, TimeUnit.SECONDS),sub2.getRef());

                new AwaitCond(
                        duration("1 second"),  // maximum wait time
                        duration("100 millis") // interval at which to check the condition
                ) {
                    protected boolean cond() {
                        return 1 == rs.underlyingActor().testCountSubscribers();
                    }
                };
//                assertTrue(1 == rs.underlyingActor().testCountSubscribers());

                Trade t5 = TradeFactory.simple(1, 500.0, 5000);
                rs.tell(t5, ActorRef.noSender());
                expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), new CalcUpdate<Trade>(RicStore.UPDATE_ID, t5));
                sub2.expectNoMsg();

                system.stop(getRef());
                new AwaitCond(
                        duration("1 second"),  // maximum wait time
                        duration("100 millis") // interval at which to check the condition
                ) {
                    protected boolean cond() {
                        return 0 == rs.underlyingActor().testCountSubscribers();
                    }
                };
                //assertTrue(0 == rs.underlyingActor().testCountSubscribers());

                Trade t6 = TradeFactory.simple(1, 600.0, 6000);
                rs.tell(t6, ActorRef.noSender());
                expectNoMsg();
                sub2.expectNoMsg();
            }};
    }
}