package com.tr.analytics.sage.akka;


import akka.actor.*;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.FromConfig;
import akka.routing.Router;
import com.tr.analytics.sage.akka.data.StartCalc;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Client extends AbstractFSMWithStash<Client.States, Client.State> {
    public static enum States { Init, Ready };

    public static final class State
    {
        ActorRef assembler;

        public State()
        {}

        public State setAssembler(ActorRef self, ActorContext context, ActorIdentity identity)
        {
            ActorRef ts = identity.getRef();
            context.watch(ts);
            return this;
        }

        boolean isReady()
        {
            return null != assembler;
        }
    }

    public Client()
    {
    }



    {
        startWith(States.Init, new State(), Duration.create(15, TimeUnit.SECONDS));

        when(States.Init,
                matchEvent(ActorIdentity.class, (event, state) -> goTo(States.Ready).using(state.setAssembler(self(), context(), event))).
                        eventEquals(StateTimeout(), (event,state) -> stop(new Failure("Client initalization timeout."), state)).
                        event(Terminated.class, (event,state) -> stop(new Failure("Assembler stopped."), state)).
                        event(StartCalc.class, (event,state) -> { stash(); return stay(); }).
                        anyEvent((e,s) -> stay().replying(new Failure("Client is still initializing..,")))
        );

        when(States.Ready,
                matchAnyEvent((event, state) -> stay())
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
        ActorRef sources = context.actorOf(FromConfig.getInstance().props(), "trade-sources");
        sources.tell(new Identify(1), self);
    }
}
