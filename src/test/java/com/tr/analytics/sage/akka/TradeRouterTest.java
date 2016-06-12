package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.tr.analytics.sage.api.Trade;
import com.tr.analytics.sage.shard.engine.TradeFactory;
import junit.framework.Assert;
import junit.framework.TestCase;
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
//            ActorRef tr = system.actorOf(Props.create(TradeRouter.class), TradeRouter.NAME);

//            assertFalse("Shouldn't have any rics.", tr.underlyingActor().testHasRic("1"));
//            assertFalse("Shouldn't have any rics.", tr.underlyingActor().testHasRics());

            // can also use JavaTestKit “from the outside”
            final JavaTestKit probe = new JavaTestKit(system);

            Trade t = TradeFactory.simple(1, 100.0, 1000);
            tr.tell(t, probe.getRef());
            assertTrue("Should have rics.", tr.underlyingActor().testHasRics());
            String ric = Long.toString(t.getQuoteId());
            assertTrue("Should have ric: " + ric, tr.underlyingActor().testHasRic(ric));

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
                        for (TradeRouter.RicStoreRefs.RicStoreRef ricStoreRef : ((TradeRouter.RicStoreRefs) m).getRicRefs())
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


            Trade t2 = TradeFactory.simple(2, 100.0, 1000);
            tr.tell(t2, getRef());
            assertTrue("Should have rics.", tr.underlyingActor().testHasRics());
            String ric2 = Long.toString(t2.getQuoteId());
            assertTrue("Should have ric: " + ric, tr.underlyingActor().testHasRic(ric));
            assertTrue("Should have ric: " + ric2, tr.underlyingActor().testHasRic(ric2));

            tr.tell(new StartCalcMultiRic("test2", "test2", 0, Arrays.asList(ric, ric2)), getRef());

            final String matches2 = new ExpectMsg<String>(Duration.create("1 second"), "Should acknowledge rics: " + Arrays.asList(ric, ric2)) {
                protected String match(Object m) {
                    if (m instanceof TradeRouter.RicStoreRefs)
                    {
                        int count = 0;
                        for (TradeRouter.RicStoreRefs.RicStoreRef ricStoreRef : ((TradeRouter.RicStoreRefs) m).getRicRefs())
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
                        for (TradeRouter.RicStoreRefs.RicStoreRef ricStoreRef : ((TradeRouter.RicStoreRefs) m).getRicRefs())
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

        }};
    }
}