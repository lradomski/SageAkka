package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.Props;
import akka.japi.function.Function3;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.tr.analytics.sage.akka.data.*;
import com.tr.analytics.sage.api.Trade;
import com.tr.analytics.sage.shard.engine.TradeFactory;
import com.tr.analytics.sage.akka.common.TestManualDispatcher;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CalcAssemblerTest extends ActorTestCaseBase {

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


    List<String> rics = Arrays.asList(ric, ric2, ric3);
    final StartCalcMultiRic req = new StartCalcMultiRic("test", "test", 1, rics);
    final String name = req.toActorName(0);
    final TestManualDispatcher testDisp = new TestManualDispatcher();



    public void test_WaitForRicStores_Timeout() {
        new JavaTestKit(system) {
            {

                final JavaTestKit client = new JavaTestKit(system);

                final TestActorRef<CalcAssembler> calcAsm = TestActorRef.create(system, Props.create(CalcAssembler.class, req, client.getRef(), 2), name);

                watch(calcAsm);
                expectTerminated(CalcAssembler.INIT_TIMEOUT.plus(EXEPECT_TO), calcAsm);
            }};
    }


    public void test_WaitForRicStores_ClientTerminate() {
        new JavaTestKit(system) {
            {

                final JavaTestKit client = new JavaTestKit(system);

                final TestActorRef<CalcAssembler> calcAsm = TestActorRef.create(system, Props.create(CalcAssembler.class, req, client.getRef(), 2), name);

                watch(calcAsm);
                system.stop(client.getRef());
                expectTerminated(Duration.create(300, TimeUnit.MILLISECONDS), calcAsm);
            }
        };
    }


    public void xx_test_WaitForRicStores_RicStorResp() {
        new JavaTestKit(system) {
            {

                final JavaTestKit tradeRouter = new JavaTestKit(system);
                final JavaTestKit ricStore0 = new JavaTestKit(system);
                final JavaTestKit ricStore1 = new JavaTestKit(system);
                final ArrayList<JavaTestKit> ricStores = new ArrayList<>(Arrays.asList(ricStore0, ricStore1)); // SEE NOTE BELOW , calcRic2));


                final JavaTestKit client = new JavaTestKit(system);

                final JavaTestKit calcRic0 = new JavaTestKit(system);
                final JavaTestKit calcRic1 = new JavaTestKit(system);
                //final JavaTestKit calcRic2 = new JavaTestKit(system); watch(calcRic2.getRef());


                final ArrayList<JavaTestKit> ricCalcs = new ArrayList<>(Arrays.asList(calcRic0, calcRic1)); // SEE NOTE BELOW , calcRic2));
                assertEquals(ricCalcs.size(), rics.size()-1); // excluding one ric

                final Iterator<JavaTestKit> iterator = ricCalcs.iterator();

                Function3<ActorRefFactory, Props, String, ActorRef> calcRicMaker = (f,p,s) -> iterator.hasNext() ? iterator.next().getRef() : null;

                // Inject test RicCalcs into calcAsm
                final TestActorRef<CalcAssembler> calcAsm = TestActorRef.create(system, Props.create(CalcAssembler.class, req, client.getRef(), 2), name);

                TradeRouter.RicStoreRefs ricRefs = new TradeRouter.RicStoreRefs(new LinkedList<>(
                        Arrays.asList(
                                new TradeRouter.RicStoreRefs.RicActorRef(rics.get(0), ricStores.get(0).getRef()),
                                new TradeRouter.RicStoreRefs.RicActorRef(rics.get(1), ricStores.get(1).getRef())
//                                , new TradeRouter.RicStoreRefs.RicActorRef(rics.get(2), ricCalcs.get(2).getRef())
                                // NOTE: intentionally excluding one ric from response as shard may not have all the RICs on it
                        ))
                );

                calcAsm.tell(ricRefs, tradeRouter.getRef());


                int idCreation = 0;
                for (TradeRouter.RicStoreRefs.RicActorRef ricRef : ricRefs.getRicRefs())
                {
                    int idRic = idCreation++;
                    StartCalcSingleRic reqSingleRic = StartCalcSingleRic.fromFor(req, idRic, ricRef.getRic());
                    ricStores.get(idRic).expectMsgEquals(EXEPECT_TO, reqSingleRic);
                }

                // when one of child calc dies - calcAsm should too
                watch(calcAsm);
                system.stop(calcRic1.getRef());
                expectTerminated(calcAsm);
            }
        };
    }

    public void xx_testKeyFlows() {
        new JavaTestKit(system) {
            {
                final JavaTestKit tradeRouter = new JavaTestKit(system);
                final JavaTestKit ricStore0 = new JavaTestKit(system);
                final JavaTestKit ricStore1 = new JavaTestKit(system);
                final ArrayList<JavaTestKit> ricStores = new ArrayList<>(Arrays.asList(ricStore0, ricStore1)); // SEE NOTE BELOW , calcRic2));

                final JavaTestKit client = new JavaTestKit(system);

                final JavaTestKit calcRic0 = new JavaTestKit(system);
                final JavaTestKit calcRic1 = new JavaTestKit(system);
                //final JavaTestKit calcRic2 = new JavaTestKit(system); watch(calcRic2.getRef());


                final ArrayList<JavaTestKit> ricCalcs = new ArrayList<>(Arrays.asList(calcRic0, calcRic1)); // SEE NOTE BELOW , calcRic2));
                assertEquals(ricCalcs.size(), rics.size()-1); // excluding one ric

                final Iterator<JavaTestKit> iterator = ricCalcs.iterator();

                Function3<ActorRefFactory, Props, String, ActorRef> calcRicMaker = (f,p,s) -> iterator.hasNext() ? iterator.next().getRef() : null;

                // Inject test RicCalcs into calcAsm
                final TestActorRef<CalcAssembler> calcAsm = TestActorRef.create(system, Props.create(CalcAssembler.class, req, client.getRef(), system.dispatcher(), calcRicMaker), name);

                TradeRouter.RicStoreRefs ricRefs = new TradeRouter.RicStoreRefs(new LinkedList<>(
                        Arrays.asList(
                                new TradeRouter.RicStoreRefs.RicActorRef(rics.get(0), ricStores.get(0).getRef()),
                                new TradeRouter.RicStoreRefs.RicActorRef(rics.get(1), ricStores.get(1).getRef())
//                                , new TradeRouter.RicStoreRefs.RicActorRef(rics.get(2), ricCalcs.get(2).getRef())
                                // NOTE: intentionally excluding one ric from response as shard may not have all the RICs on it
                        ))
                );

                calcAsm.tell(ricRefs, tradeRouter.getRef());
                client.expectMsgEquals(EXEPECT_TO, ricRefs); // forwards to assembler for book-keeping

                Trade t_0 = TradeFactory.simple(quoteId, 10, 100);
                Trade t2_0 = TradeFactory.simple(quoteId, 20, 200);
                Trade[] trades = new Trade[]{t_0, t2_0};
                TradeTotals tt = TradeTotals.from(new RicStore.Trades(trades, trades.length));

                // response from calcRic0
                {
                    TradeTotals resp = TradeTotals.from(RicStore.Trades.from(new Trade[]{t_0}));
                    calcAsm.tell(new CalcResult<>(0, resp), calcRic0.getRef());
                    client.expectNoMsg(EXEPECT_TO); // still gathering responses
                }

                // now follow-up trade from calcRic0
                calcAsm.tell(new CalcUpdate<>(0, TradeTotals.from(t2_0)),calcRic0.getRef());
                client.expectNoMsg(EXEPECT_TO); // still missing response from second rics



                Trade t3_1 = TradeFactory.simple(quoteId2, 30, 300);
                Trade t4_1 = TradeFactory.simple(quoteId2, 40, 400);
                Trade[] trades2 = new Trade[]{t3_1, t4_1};

                // response from calcRic1
                {
                    TradeTotals resp = TradeTotals.from(RicStore.Trades.from(trades2));
                    calcAsm.tell(new CalcResult<>(1, resp), calcRic1.getRef());
                }

                // now we have responses from both (all) rics so first response should be sent out
                TradeTotals respTT = TradeTotals.from(t_0).makeUpdated(t2_0).makeUpdated(t3_1).makeUpdated(t4_1);
                client.expectMsgEquals(new CalcResult<>(req.getId(), respTT));

                // and now we just stream updates
                Trade t5_1 = TradeFactory.simple(quoteId2, 50, 500);
                calcAsm.tell(new CalcUpdate<>(1, TradeTotals.from(t5_1)),calcRic1.getRef());
                client.expectMsgEquals(new CalcUpdate<>(req.getId(), TradeTotals.from(t5_1)));

                Trade t6_0 = TradeFactory.simple(quoteId, 60, 600);
                calcAsm.tell(new CalcUpdate<>(0, TradeTotals.from(t6_0)),calcRic1.getRef());
                client.expectMsgEquals(new CalcUpdate<>(req.getId(), TradeTotals.from(t6_0)));

                // now calcRic1 sends full response (unsolicitated refresh)
                // calcAsm asks other calcRics (calcRic0 here) to referesh as well
                {
                    TradeTotals resp = TradeTotals.from(RicStore.Trades.from(new Trade[]{})); // resests calcRic1
                    calcAsm.tell(new CalcResult<>(1, resp), calcRic1.getRef());
                    client.expectNoMsg(EXEPECT_TO); // calcAsm will keep it

                    StartCalcSingleRic reqSingleRic = StartCalcSingleRic.fromFor(req, 0, rics.get(0));
                    calcRic0.expectMsgEquals(EXEPECT_TO, reqSingleRic); // refresh for this
                    calcRic1.expectNoMsg(EXEPECT_TO); // this one is skipped as it originate refreshes sequence with its own refresh
                }

                // keep forwarding updates as-is as continuing to wait for the refresh (from calcRic0)
                Trade t7_1 = TradeFactory.simple(quoteId2, 50, 500);
                calcAsm.tell(new CalcUpdate<>(1, TradeTotals.from(t5_1)),calcRic1.getRef());
                client.expectMsgEquals(new CalcUpdate<>(req.getId(), TradeTotals.from(t5_1)));

                Trade t8_0 = TradeFactory.simple(quoteId, 60, 600);
                calcAsm.tell(new CalcUpdate<>(0, TradeTotals.from(t6_0)),calcRic1.getRef());
                client.expectMsgEquals(new CalcUpdate<>(req.getId(), TradeTotals.from(t6_0)));

                {
                    // now calcRic0 refreshes - results of all the trades it sent out thus far
                    TradeTotals resp = TradeTotals.from(RicStore.Trades.from(new Trade[]{t_0, t2_0, t6_0, t8_0}));
                    calcAsm.tell(new CalcResult<>(0, resp), calcRic0.getRef());

                    // now we have all responses again - send refresh
                    TradeTotals respShard = TradeTotals.from(RicStore.Trades.from(new Trade[]{t_0, t2_0, t6_0, t8_0, t7_1})); // resests calcRic1
                    client.expectMsgEquals(new CalcResult<>(req.getId(), respShard));
                    //client.expectNoMsg(EXEPECT_TO); // calcAsm will keep it
                }
            }
        };
    }

}