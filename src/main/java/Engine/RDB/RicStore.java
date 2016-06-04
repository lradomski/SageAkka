package Engine.RDB;

import Engine.Data.StartCalc;
import Engine.Data.Trade;
import Engine.Data.Trades;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Router;
import sample.hello.Greeter;

import static java.util.Arrays.copyOf;


public class RicStore extends UntypedActor {
    final static int initSize = 1024;
    final static int growthDelta = 0;
    final static float growthFactor = 0.3f;

    private Trade[] trades = null;
    private int nextSlot = 0;

    private Router router = null;


    public RicStore()
    {
        trades = new Trade[initSize];
        router = new Router(new BroadcastRoutingLogic());

    }

    @Override
    public void onReceive(Object m) throws Exception {
        if (m instanceof StartCalc)
        {
            StartCalc sc = (StartCalc)m;

            // TODO: instantiate class based on "className"
            final ActorRef calc = getContext().actorOf(Props.create(RicCalc.class), sc.getClassName());
            router.addRoutee(calc);
            getContext().watch(calc);

            calc.tell(makeTrades(), getSender() ); // pass sender!
            getSender().tell(calc, getSelf());
        }
        else if (m instanceof Trade)
        {
            ensureStorage();
            trades[nextSlot++] = (Trade)m;
            router.route(m, getSelf());
        }

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
