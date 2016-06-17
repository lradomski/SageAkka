package com.tr.analytics.sage.akka;

import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.tr.analytics.sage.akka.data.TestVisitor;
import com.tr.analytics.sage.akka.data.Trade;
import com.tr.analytics.sage.akka.data.TradeFactory;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TradeRouterTest extends ActorTestCaseBase {

    protected void setUp() throws Exception {
        super.setUp();

    }

    public void tearDown() throws Exception {
        super.tearDown();

    }

    public void testTradeAndAcknowledgment()
    {
        new JavaTestKit(system) {{
            TestActorRef<TradeRouter> tr = TestActorRef.create(system, Props.create(TradeRouter.class), TradeRouter.NAME);

            assertFalse("Shouldn't have any rics.", tr.underlyingActor().testHasRic("1"));
            assertFalse("Shouldn't have any rics.", tr.underlyingActor().testHasRics());

            // can also use JavaTestKit “from the outside”
            final JavaTestKit probe = new JavaTestKit(system);

            Trade t = TradeFactory.simple(1, 100.0, 1000);
            tr.tell(t, probe.getRef());
            assertTrue("Should have rics.", tr.underlyingActor().testHasRics());
            String ric = Long.toString(t.getQuoteId());
            assertTrue("Should have ric: " + ric, tr.underlyingActor().testHasRic(ric));
            assertTrue("Should have 1 child.", 1 == tr.underlyingActor().context().children().size());

            // new RicStore is first child
            tr.underlyingActor().testGetRicStore(ric).tell(new TestVisitor(RicStore.TESTVERB_LAST_TRADE), getRef());
            expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), t);


            tr.tell(new StartCalcMultiRic("test", "test", 0, Arrays.asList(ric)), getRef());

//            new AwaitCond(
//                    duration("1 second"),  // maximum wait time
//                    duration("100 millis") // interval at which to check the condition
//            ) {
//                protected boolean cond() {
//                    // typically used to wait for something to start up
//                    return msgAvailable();
//                }
//            };

            final String matches = new ExpectMsg<String>(Duration.create(1, TimeUnit.SECONDS), "Should acknowledge ric: " + ric) {
                protected String match(Object m) {
                    if (m instanceof TradeRouter.RicStoreRefs)
                    {
                        int count = 0;
                        for (TradeRouter.RicStoreRefs.RicActorRef ricStoreRef : ((TradeRouter.RicStoreRefs) m).getRicRefs())
                        {
                            if (!ricStoreRef.getRic().equals(ric))
                            {
                                throw noMatch();
                            }

                            ++count;
                        }

                        return Integer.toString(count);
                    } else {
                        throw noMatch();
                    }
                }
            }.get();
            assertTrue("Should acknowledge one and only ric:" + ric, matches.equals("1"));



            Trade t2 = TradeFactory.simple(2, 200.0, 3000);
            tr.tell(t2, getRef());
            assertTrue("Should have rics.", tr.underlyingActor().testHasRics());
            String ric2 = Long.toString(t2.getQuoteId());
            assertTrue("Should have ric: " + ric, tr.underlyingActor().testHasRic(ric));
            assertTrue("Should have ric: " + ric2, tr.underlyingActor().testHasRic(ric2));
            assertTrue("Should have 2 children.", 2 == tr.underlyingActor().context().children().size());

            tr.underlyingActor().testGetRicStore(ric).tell(new TestVisitor(RicStore.TESTVERB_LAST_TRADE), getRef());
            expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), t);

            tr.underlyingActor().testGetRicStore(ric2).tell(new TestVisitor(RicStore.TESTVERB_LAST_TRADE), getRef());
            expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), t2);

            tr.tell(new StartCalcMultiRic("test2", "test2", 0, Arrays.asList(ric, ric2)), getRef());

            final String matches2 = new ExpectMsg<String>(Duration.create("1 second"), "Should acknowledge rics: " + Arrays.asList(ric, ric2)) {
                protected String match(Object m) {
                    if (m instanceof TradeRouter.RicStoreRefs)
                    {
                        int count = 0;
                        for (TradeRouter.RicStoreRefs.RicActorRef ricStoreRef : ((TradeRouter.RicStoreRefs) m).getRicRefs())
                        {
                            if (!(ricStoreRef.getRic().equals(ric) || ricStoreRef.getRic().equals(ric2)))
                            {
                                throw noMatch();
                            }

                            ++count;
                        }

                        return Integer.toString(count);
                    } else {
                        throw noMatch();
                    }
                }
            }.get();
            assertTrue("Should acknowledge one and only ric:" + ric, matches2.equals("2"));


            tr.tell(new StartCalcMultiRic("test2A", "test2A", 0, Arrays.asList(ric, ric2, "invalid")), getRef());

            final String matches2A = new ExpectMsg<String>(Duration.create("1 second"), "Should still acknowledge rics: " + Arrays.asList(ric, ric2)) {
                protected String match(Object m) {
                    if (m instanceof TradeRouter.RicStoreRefs)
                    {
                        int count = 0;
                        for (TradeRouter.RicStoreRefs.RicActorRef ricStoreRef : ((TradeRouter.RicStoreRefs) m).getRicRefs())
                        {
                            if (!(ricStoreRef.getRic().equals(ric) || ricStoreRef.getRic().equals(ric2)))
                            {
                                throw noMatch();
                            }

                            ++count;
                        }

                        return Integer.toString(count);
                    } else {
                        throw noMatch();
                    }
                }
            }.get();
            assertTrue("Should acknowledge one and only ric:" + ric, matches2A.equals("2"));

            Trade t3 = TradeFactory.simple((int)t.getQuoteId(), 101.0, 1001);
            tr.tell(t3, getRef());
            assertTrue("Should have 2 children.", 2 == tr.underlyingActor().context().children().size());

            tr.underlyingActor().testGetRicStore(ric).tell(new TestVisitor(RicStore.TESTVERB_LAST_TRADE), getRef());
            expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), t3);

            tr.underlyingActor().testGetRicStore(ric2).tell(new TestVisitor(RicStore.TESTVERB_LAST_TRADE), getRef());
            expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), t2);

        }};
    }
}