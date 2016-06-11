package com.tr.analytics.sage.akka;


import com.tr.analytics.sage.akka.data.CalcResult;
import com.tr.analytics.sage.akka.data.SageIdentify;
import com.tr.analytics.sage.akka.data.SageIdentity;
import com.tr.analytics.sage.akka.data.StartCalc;

import akka.actor.*;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.FromConfig;
import akka.routing.Router;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class TradeSource extends AbstractFSMWithStash<TradeSource.States, TradeSource.State> {

    public static final String NAME = "trade-source";

    public static enum States { Init };

    public static final class State
    {}

    public TradeSource()
    {
    }

    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    {
        startWith(States.Init, new State());

        when(States.Init,
                    matchEvent(SageIdentify.class, (event,state) -> handleIdentify(event))
        );


        whenUnhandled(
                matchAnyEvent((event, state) -> {
                    log().warning("TradeSource received unhandled event {} in state {}/{}",
                            event, stateName(), state);
                    return stay();
                })
        );
        // logging
        onTransition(
                matchState(null, null, (from,to) -> log().debug("From: " + from.toString() + ", to: " + to.toString() + ", data: " + stateData()))
        );

        // init
        onTransition(
                matchState(null, States.Init, (from,to) -> {})
        );

//        onTransition(
//                matchState(null, States.Init, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }

    private FSM.State<States, State> handleIdentify(SageIdentify event) {
        context().watch(sender());
        return stay().replying(SageIdentity.from(event, self()));
    }
}
