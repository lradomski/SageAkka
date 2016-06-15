package com.tr.analytics.sage.akka;


import akka.actor.*;

import com.tr.analytics.sage.akka.data.*;
import scala.concurrent.duration.*;//Duration;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Client extends AbstractFSMWithStash<Client.States, Client.State> {
    public static final String NAME = "com.tr.analytics.sage.akka.ScriptDriver";

    public static enum States { Init, Ready };

    public static final class State
    {
        final HashMap<Integer, Long> durations = new HashMap<>();
        ActorRef assembler;
        int id = 0;

        public State()
        {}

        public State setAssembler(ActorRef self, ActorContext context, SageIdentity identity)
        {
            ActorRef ts = identity.getRef();
            context.watch(ts);
            assembler = ts;
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
        startWith(States.Init, new State(), Duration.create(3, TimeUnit.SECONDS));

        when(States.Init,
                matchEvent(SageIdentity.class, (event, state) -> {
                    unstashAll();
                    return goTo(States.Ready).using(state.setAssembler(self(), context(), event));
                }).
                eventEquals(StateTimeout(), (event,state) -> stop(new Failure("SimpleClient initialization timeout."), state)).
                event(Terminated.class, (event,state) -> stop(new Failure("Assembler stopped."), state)).
                event(StartCalc.class, (event,state) -> { stash(); return stay(); })
        );

        when(States.Ready,
                matchEvent(StartCalc.class, (event,state) -> {
                    System.out.println("Got request: " + event);
                    long nanoTime = System.nanoTime();
                    state.assembler.tell(event, self());
                    state.durations.put(event.getId(), System.nanoTime());
                    return stay();
                }).
                event(CalcResultCore.class, (event, state) -> {
                    Long endTime = System.nanoTime();
                    double duration = 0L;
                    if (state.durations.containsKey(event.getId()))
                    {
                        Long beginTime = state.durations.get(event.getId());
                        duration = (endTime-beginTime) / (1000*1000.0); // in millis
                    }
                    System.out.println("Got response: " + event + ", duration:" + new DecimalFormat("#.####").format(duration) + "ms"); //String.format("%s",duration));
                    return stay(); //.replying(event);
                }).
                event(CalcUpdateCore.class, (event, state) -> {
                    System.out.println("Got update: " + event);
                    return stay(); //.replying(event);
                })
        );


        whenUnhandled(
                matchAnyEvent((event, state) -> {
                    log().warning("SimpleClient received unhandled event {} in state {}/{}",
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
                matchState(null, States.Init, (from,to) -> IdentifyAssembler(self(), context()))
        );

//        onTransition(
//                matchState(null, States.Idle, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }

    static void IdentifyAssembler(ActorRef self, ActorContext context)
    {
        context.system().actorSelection("/user/" + Assembler.NAME).tell(new SageIdentify(1), self);
    }
}
