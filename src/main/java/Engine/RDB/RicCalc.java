package Engine.RDB;

import Engine.Data.Trade;
import Engine.Data.Trades;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.japi.function.Procedure;

/**
 * Created by luke on 6/4/16.
 */
public class RicCalc extends UntypedActor {

    public class Result
    {
        private final long totalVolume;
        private final double totalTurnover;

        public Result(long totalVolume, double totalTurnover)
        {
            this.totalVolume = totalVolume;
            this.totalTurnover = totalTurnover;
        }

        public long getTotalVolume() {
            return totalVolume;
        }

        public double getTotalTurnover() {
            return totalTurnover;
        }
    }

    private final Procedure<Object> initState;
    private final Procedure<Object> waitResponseState;
    ActorRef resultReceiver = null;

    public RicCalc()
    {
        initState = new Procedure<Object>() {
            public void apply(Object m) throws Exception {
                if (m instanceof Trades) {
                    resultReceiver = getSender();
                    // TODO: long response here
                    getContext().unbecome();
                }
                else
                {
                    unhandled(m);
                }
            }
        };

        waitResponseState = new Procedure<Object>() {
            public void apply(Object m) throws Exception {
                if (m instanceof Trades) {
                    resultReceiver = getSender();
                    // TODO: long response here
                    getContext().unbecome();
                }
                else
                {
                    unhandled(m);
                }
            }
        };
    }

    @Override
    public void preStart() throws Exception {
        //getContext().become(, false); // add behavior on top instead of replacing
    }

    @Override
    public void onReceive(Object m) throws Exception {
        if (m instanceof Trades)
        {


        }
        else if (m instanceof Trade)
        {

        }
        else
        {
            unhandled(m);
        }


    }
}
