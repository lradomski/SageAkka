package com.tr.analytics.sage.akka;


import akka.actor.*;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.FromConfig;
import akka.routing.Router;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Assembler extends AbstractLoggingFSM<Assembler.States, Assembler.State> {
    public static enum States { Init, Ready };

    public static final class State
    {
        Router router = new Router(new BroadcastRoutingLogic());

        public State()
        {}

        public State addShard(ActorRef self, ActorContext context, ActorIdentity identity)
        {
            ActorRef ts = identity.getRef();
            context.watch(ts);
            router = router.addRoutee(ts);
            return this;
        }

        boolean isReady()
        {
            return router.routees().length() != 0;
        }
    }

    public Assembler()
    {
    }



    {
        startWith(States.Init, new State(), Duration.create(15, TimeUnit.SECONDS));

        when(States.Init,
                matchEvent(ActorIdentity.class, (event, state) -> goTo(States.Ready).using(state.addShard(self(), context(), event))).
                        eventEquals(StateTimeout(), (event,state) -> stop(new Failure("Assembler initalization timeout."), state)).
                        event(Terminated.class, (event,state) -> stop(new Failure("Shard stopped."), state)).
                        anyEvent((e,s) -> stay().replying(new Failure("Assembler is still initializing..,")))
        );

        when(States.Ready,
                matchAnyEvent((event, state) -> stay()).
                        event(Terminated.class, (event,state) -> stop(new Failure("Shard stopped."), state))
        );

        // logging
        onTransition(
                matchState(null, null, (from,to) -> System.out.println("from: " + from.toString() + ", to: " + to.toString() + ", data: " + stateData()))

        );

        // init
        onTransition(
                matchState(null, States.Init, (from,to) -> IdentfySources(self(), context()))
        );

//        onTransition(
//                matchState(null, States.Init, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }

    static void IdentfySources(ActorRef self, ActorContext context)
    {
        ActorRef sources = context.actorOf(FromConfig.getInstance().props(), "shard");
        sources.tell(new Identify(1), self);
    }
}
