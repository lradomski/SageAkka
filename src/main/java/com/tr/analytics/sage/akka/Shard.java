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

public class Shard extends AbstractFSMWithStash<Shard.States, Shard.State> {

    public static final String NAME = "shard";

    public static enum States { Init, Ready };

    public static final class State
    {
        Router router = new Router(new BroadcastRoutingLogic());

        public State()
        {}

        public State addTradeSource(ActorRef self, ActorContext context, ActorRef ts) throws Exception {
            context.watch(ts);
            router = router.addRoutee(ts);
            return this;
        }

        boolean isReady()
        {
            return router.routees().length() != 0;
        }
    }

    public Shard()
    {
    }

    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    {
        startWith(States.Init, new State(), Duration.create(15, TimeUnit.SECONDS));

        when(States.Init,
                matchEvent(SageIdentity.class, (event, state) -> goTo(States.Ready).using(handleTradeSource(event, state, true))).
                eventEquals(StateTimeout(), (event,state) -> stop(new Failure("Shard initialization timeout."), state)).
                event(SageIdentify.class, (event,state) -> handleIdentify(event)).
                event(Terminated.class, (event,state) -> stop(new Failure("Trade Source stopped."), state)).
                event(StartCalc.class, (event,state) -> { stash(); return stay();   })
        );

        when(States.Ready,
                matchEvent(SageIdentity.class, (event, state) -> stay().using(handleTradeSource(event, state, false))).
                event(StartCalc.class, (event, state) -> {
                    //state.shards.route(event, sender()); // preserve the sender !
                    return stay().replying(CalcResult.from(event));
                }).
                event(SageIdentify.class, (event,state) -> handleIdentify(event)).
                event(Terminated.class, (event,state) -> stop(new Failure("Shard stopped."), state))
        );

        whenUnhandled(
                matchAnyEvent((event, state) -> {
                            log().warning("Shard received unhandled event {} in state {}/{}",
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
                matchState(null, States.Init, (from,to) -> IdentifySources(self(), context()))
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

    private State handleTradeSource(SageIdentity event, State state, boolean unstash) throws Exception {
        state.addTradeSource(self(), context(), event.getRef());
        if (unstash) unstashAll();
        log().info("Trade Source detected");
        return state;
    }

    static void IdentifySources(ActorRef self, ActorContext context) {
        ActorRef sources = context.system().actorOf(FromConfig.getInstance().props(), "trade-sources");
        sources.tell(new SageIdentify(1), self);
    }
}
