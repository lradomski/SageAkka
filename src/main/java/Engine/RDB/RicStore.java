package Engine.RDB;

import Engine.Data.StartCalc;
import Engine.Data.Trade;
import Engine.Data.Trades;
import Engine.Source.TestTradeSource;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;

import static java.util.Arrays.copyOf;


public class RicStore extends UntypedActor {
    final static int initSize = 1024;
    final static int growthDelta = 0;
    final static float growthFactor = 0.3f;

    private Trade[] trades = null;
    private int nextSlot = 0;

    private Router router = null;
    private String ric = null;
    private ActorRef tradeSource;

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

    public RicStore(String ric, ActorRef tradeSource)
    {
        this.trades = new Trade[initSize];
        this.router = new Router(new BroadcastRoutingLogic());
        this.tradeSource = tradeSource;

    }

    public static Props props(final String ric, final ActorRef tradeSource) {
        return Props.create(new Creator<RicStore>() {

            public RicStore create() throws Exception {
                return new RicStore(ric, tradeSource);
            }
        });
    }

    @Override
    public void preStart() throws Exception {
        tradeSource.tell(new TestTradeSource.Command(TestTradeSource.Verb.SUBSCRIBE, this.ric), getSelf());
        getContext().watch(tradeSource);
    }

    @Override
    public void onReceive(Object m) throws Exception {
        if (m instanceof StartCalc)
        {
            StartCalc sc = (StartCalc)m;

            // TODO: instantiate class based on "className"
            final ActorRef calc = getContext().actorOf(Props.create(RicCalc.class), sc.getCalcName());
            router.addRoutee(calc);
            getContext().watch(calc);

            calc.tell(makeTrades(), getSender() ); // passing sender, not self !
            getSender().tell( new Ack(ric, calc), getSelf());
        }
        else if (m instanceof Trade)
        {
            ensureStorage();
            trades[nextSlot++] = (Trade)m;
            router.route(m, getSelf());
        }
        else if (m instanceof TestTradeSource.Command)
        {
            // ignore for now
        }
        else {
            unhandled(m);
        }

    }

    @Override
    public void postStop() throws Exception {
        tradeSource.tell(new TestTradeSource.Command(TestTradeSource.Verb.UNSUBSCRIBE, this.ric), getSelf());
        getContext().unwatch(tradeSource);

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
