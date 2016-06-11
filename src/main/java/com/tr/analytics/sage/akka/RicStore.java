package com.tr.analytics.sage.akka;

import com.tr.analytics.sage.akka.data.StartCalc;
import Engine.Data.Trades;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import com.tr.analytics.sage.api.Trade;

import static java.util.Arrays.copyOf;


public class RicStore extends UntypedActor {
    final static int initSize = 1024;
    final static int growthDelta = 0;
    final static float growthFactor = 0.3f;

    private Trade[] trades = null;
    private int nextSlot = 0;

    private Router router = null;
    private String ric = null;

    public static class Ack
    {
        private final String ric;
        private final ActorRef calc;

        public Ack(String ric, ActorRef calc)
        {
            this.ric = ric;
            this.calc = calc;
        }

        public String getRic() {
            return ric;
        }

        public ActorRef getCalc() {
            return calc;
        }
    }

    public RicStore(String ric)
    {
        this.ric = ric;
        this.trades = new Trade[initSize];
        this.router = new Router(new BroadcastRoutingLogic());
    }

    public static Props props(final String ric) {
        return Props.create((Creator<RicStore>) () -> new RicStore(ric));
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

            // TODO: instantiate class based on "className"
            final ActorRef calc = null;//getContext().actorOf(Props.create(RicCalc.class), sc.getCalcName());
            router.addRoutee(calc);

            calc.tell(makeTrades(), getSender() ); // passing sender, not self !
            getSender().tell( new Ack(ric, calc), getSelf());
        }
        else if (m instanceof Trade)
        {
            System.out.println("RicStore(" + ric + ")+=" + m);
            ensureStorage();
            trades[nextSlot++] = (Trade)m;
            router.route(m, getSelf());
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

    private Trades makeTrades()
    {
        return new Trades(trades,nextSlot);
    }
}
