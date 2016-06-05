package Engine.Source;

import Engine.Data.Trade;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.contrib.throttle.Throttler;
import akka.contrib.throttle.TimerBasedThrottler;
import akka.dispatch.ControlMessage;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import sample.hello.Greeter;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestTradeSource extends UntypedActor {
    private final HashMap<String, Router> ricSinks = new HashMap<String, Router>();
    private final List<String> rics = new ArrayList<String>();
    private final Random rand = new Random();
    boolean isAlive = true;

    int nextRic = 0;

    public enum Verb { SUBSCRIBE, UNSUBSCRIBE };

    public static class Command implements ControlMessage {
        private Verb verb;
        private String ric;

        public Command(Verb verb, String ric)
        {
            this.verb = verb;
            this.ric = ric;
        }

        public Verb getVerb() {
            return verb;
        }

        public String getRic() {
            return ric;
        }
    }

    public enum Tick {
        TICK
    }

    @Override
    public void preStart() throws Exception {

        Thread t = new Thread(new Runnable() {

            public void run(){
                // The throttler for this example, setting the rate
                int rateMs = 1; //100;
                int intervalMs = 50;
                ActorRef throttler = getContext().actorOf(Props.create(TimerBasedThrottler.class,
                        new Throttler.Rate(intervalMs*rateMs, Duration.create(intervalMs, TimeUnit.MILLISECONDS))));
                // Set the target
                throttler.tell(new Throttler.SetTarget(getSelf()), null);
                //throttler = greeter;

                // tell it to perform the greeting
                //greeter.tell(Greeter.Msg.GREET, getSelf());
                //for(int i = 0; i < 10*1000*rateMs; i++)
                while(isAlive)
                {
                    throttler.tell(Tick.TICK, getSelf());
                }
            }
        });

        t.start();
    }

    @Override
    public void postStop() throws Exception {
        isAlive = false;
    }

    @Override
    public void onReceive(Object m) throws Exception
    {
        if (m instanceof Command)
        {
            Command c = (Command)m;
            switch (c.getVerb())
            {
                case SUBSCRIBE:
                    ensureRouter(c.getRic()).addRoutee(getSender());
                    this.getContext().watch(getSender());
                    this.getSender().tell(c, getSelf());
                    break;

                case UNSUBSCRIBE:
                    if (0 == ensureRouter(c.getRic()).removeRoutee(getSender()).routees().length())
                    {
                        ricSinks.remove(c.getRic());
                        rics.remove(c.getRic());
                    }

                    this.getContext().unwatch(getSender());
                    this.getSender().tell(c, getSelf());
                    break;

                default:
                    unhandled(m);
            }

        }
        else if (m instanceof Tick)
        {
            if (nextRic >= rics.size())
            {
                nextRic = 0;
            }

            String ric = rics.get(nextRic);
            Router r = ricSinks.get(ric);
            if (null != r)
            {
                int venueId = rand.nextInt();
                double price = rand.nextDouble() * 100;
                double tradeVolume  = rand.nextDouble() * 10*1000;;
                byte tradeClassification = (byte)rand.nextInt(8);
                String qualifiers = "ABC";
                Trade t = new Trade(ric, ric.hashCode(), venueId, price, new Date(), tradeVolume, tradeClassification, qualifiers);
                r.route(t, getSelf());
            }
        }
        else
        {
            unhandled(m);
        }

    }

    private Router ensureRouter(String ric)
    {
        Router r = ricSinks.get(ric);
        if (null == r)
        {
            r = new Router(new BroadcastRoutingLogic());
            ricSinks.put(ric, r);
            rics.add(ric);
        }

        return r;
    }

}
