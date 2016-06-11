package com.tr.analytics.sage.akka;


import com.tr.analytics.sage.akka.data.SageIdentify;
import com.tr.analytics.sage.akka.data.SageIdentity;
import com.tr.analytics.sage.akka.data.StartCalc;

import akka.actor.*;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.FromConfig;
import akka.routing.Router;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Assembler extends AbstractFSMWithStash<Assembler.States, Assembler.State> {

    public static final String NAME = "assembler";
    public static final String SHARDS_NAME = "shards";

    public static enum States { Init, Ready };


    public static final class State
    {
        Router shards = new Router(new BroadcastRoutingLogic());

        public State()
        {}

        public State addShard(ActorRef self, ActorContext context, ActorRef shard)
        {
            context.watch(shard);
            shards = shards.addRoutee(shard);
            return this;
        }

        boolean isReady()
        {
            return shards.routees().length() != 0;
        }
    }

    public Assembler()
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
                matchEvent(SageIdentity.class, (event, state) -> goTo(States.Ready).using(handleShard(event, state, true))).
                eventEquals(StateTimeout(), (event,state) -> stop(new Failure("Assembler initialization timeout."), state)).
                event(SageIdentify.class, (event,state) -> handleIdentify(event)).
                event(Terminated.class, (event,state) -> stop(new Failure("Shard stopped."), state)).
                event(StartCalc.class, (event,state) -> { stash(); return stay(); })
        );

        when(States.Ready,
                matchEvent(StartCalc.class, (event, state) -> {
                    state.shards.route(event, sender()); // preserve the sender !
                    return stay();
                }).
                event(SageIdentify.class, (event,state) -> handleIdentify(event)).
                event(Terminated.class, (event,state) -> stop(new Failure("Shard stopped."), state)).
                anyEvent((event, state) -> stay())
        );


        whenUnhandled(
                matchAnyEvent((event, state) -> {
                    log().warning("Assembler received unhandled event {} in state {}/{}",
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
                matchState(null, States.Init, (from,to) -> IdentifyShards(self(), context()))
        );

//        onTransition(
//                matchState(null, States.Init, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }

    private State handleShard(SageIdentity event, State state, boolean unstash) throws Exception {
        state.addShard(self(), context(), event.getRef());
        if (unstash) unstashAll();
        log().info("Shard detected");
        return state;
    }

    static void IdentifyShards(ActorRef self, ActorContext context)
    {
        ActorRef sources = context.actorOf(FromConfig.getInstance().props(), SHARDS_NAME);
        sources.tell(new SageIdentify(1), self);
    }


    private FSM.State<States, State> handleIdentify(SageIdentify event) {
        context().watch(sender());
        return stay().replying(SageIdentity.from(event, self()));
    }}
