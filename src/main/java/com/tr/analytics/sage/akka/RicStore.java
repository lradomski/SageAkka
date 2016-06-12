package com.tr.analytics.sage.akka;

import akka.actor.Terminated;
import com.tr.analytics.sage.akka.data.CalcResult;
import com.tr.analytics.sage.akka.data.CalcUpdate;
import com.tr.analytics.sage.akka.data.StartCalc;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import com.tr.analytics.sage.akka.data.TestVisitor;
import com.tr.analytics.sage.api.Trade;

import java.util.HashSet;
import java.util.Iterator;

import static java.util.Arrays.copyOf;


public class RicStore extends UntypedActor {
    final static int initSize = 1024;
    final static int growthDelta = 0;
    final static float growthFactor = 0.3f;

    public static final String TESTVERB_LAST_TRADE = "last";
    public static final int UPDATE_ID = -1;

    private Trade[] trades = null;
    private int nextSlot = 0;

    private Router router = null;
    private final String ric;
    private final HashSet<ActorRef> subscribers = new HashSet<>();

    public static class Trades {

        private Trade[] trades;
        private int endExclusive;

        public Trades(Trade[] trades, int endExclusive)
        {
            this.trades = trades;
            this.endExclusive = endExclusive;
        }

//        public Trade[] getTrades() {
//            return trades;
//        }
//
        public int getCount() {
            return endExclusive;
        }

        public Iterable<Trade> getTrades()
        {
            return new Iterable<Trade>() {
                @Override
                public Iterator<Trade> iterator() {
                    return new Iterator<Trade>() {
                        int i = 0;

                        @Override
                        public boolean hasNext() {
                            return i < endExclusive;
                        }

                        @Override
                        public Trade next() {
                            return trades[i++];
                        }
                    };
                }
            };
//            return new Iterable<Trade>
//            return this.trades;
        }
    }

    public RicStore(String ric)
    {
        this.ric = ric;
        this.trades = new Trade[initSize];
        this.router = new Router(new BroadcastRoutingLogic());
    }

    public static Props props(final String ric) {
        return Props.create(RicStore.class,(Creator<RicStore>) () -> new RicStore(ric));
    }

    @Override
    public void preStart() throws Exception {
        System.out.println("++RicStore(" + ric + ")");
    }

    @Override
    public void onReceive(Object m) throws Exception {
        if (m instanceof StartCalc)
        {
            StartCalc sc = (StartCalc)m;

            int before = subscribers.size();
            subscribers.add(sender());
            if (before != subscribers.size()) {
                context().watch(sender());
                System.out.println("RicStore(" + ric + ")+=" + sender());
                router = router.addRoutee(sender());
            }

            sender().tell(makeTrades(sc.getId()), self() );
        }
        else if (m instanceof Trade)
        {
            System.out.println("RicStore(" + ric + ")+=" + m);
            ensureStorage();
            Trade trade = (Trade)m;
            trades[nextSlot++] = trade;

            // For most rics there won't be subscribers so don't try to even route then
            if (0 < subscribers.size())
            {
                router.route(new CalcUpdate<Trade>(UPDATE_ID, trade), self());
            }
        }
        else if (m instanceof Terminated)
        {
            System.out.println("RicStore(" + ric + ")-=" + m);
            ActorRef actorRef = ((Terminated) m).actor();

            subscribers.remove(actorRef);
            router = router.removeRoutee(actorRef);
        }
        else if (m instanceof TestVisitor)
        {
            TestVisitor v = ((TestVisitor) m);
            if (v.getVerb().equals(TESTVERB_LAST_TRADE))
            {
                sender().tell(0 == nextSlot ? null : trades[nextSlot-1], self());
            }
            else
            {
                sender().tell(new TestVisitor(null, null), self());
            }

        }
        else {
            unhandled(m);
        }

    }

    @Override
    public void postStop() throws Exception {

    }

    private void ensureStorage()
    {
        if (trades.length == nextSlot)
        {
            int growth = growthDelta + (int)(trades.length*growthFactor);
            trades = copyOf(trades, trades.length + growth);
        }

    }

    private CalcResult<Trades> makeTrades(int id)
    {
        // array is always appended so it's the part already written to is safe to pass to other actors/threads
        return new CalcResult<>(id, makeTrades());
    }

    private Trades makeTrades() {
        return new Trades(trades,nextSlot);
    }

    public int testGetTradeCount()
    {
        return nextSlot;
    }

    public Trade testGetLastTrade()
    {
        return nextSlot > 0 ? trades[nextSlot-1] : null;
    }

    public int testCountSubscribers()
    {
        return subscribers.size();
    }

    public Trades testMakeTrades()
    {
        return makeTrades();
    }

}
