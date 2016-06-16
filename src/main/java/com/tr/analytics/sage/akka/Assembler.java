package com.tr.analytics.sage.akka;


import com.tr.analytics.sage.akka.data.SageIdentify;
import com.tr.analytics.sage.akka.data.SageIdentity;
import com.tr.analytics.sage.akka.data.StartCalc;

import akka.actor.*;
import akka.routing.FromConfig;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Assembler extends AbstractFSMWithStash<Assembler.States, Assembler.State> {

    public static final String NAME = "assembler";
    public static final String SHARDS_NAME = "shards";

    public static enum States { Init, Ready };


    public static final class State
    {
        // TODO: refactor into router of individual shards' ActorRefs
        final ActorRef shards;
        int countShards = 0;
        int idNext = 0;

        public State(ActorRef shards)
        {
            this.shards = shards;
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
        startWith(States.Init, new State(IdentifyShards()), Duration.create(15, TimeUnit.SECONDS));

        when(States.Init,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Assembler initialization timeout."))).
                event(SageIdentity.class, (event, state) -> goTo(States.Ready).using(handleShard(event, state, true))).
                event(SageIdentify.class, (event,state) -> handleIdentify(event)).
                event(Terminated.class, (event,state) -> stop(new Failure("Shard stopped."), state)).
                event(StartCalc.class, (event,state) -> { stash(); return stay(); })
        );

        when(States.Ready,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure("Shard stopped."))).
                event(StartCalcMultiRic.class, (event, state) -> launchRequest(event, state)).
                event(SageIdentity.class, (event, state) -> stay().using(handleShard(event, state, false))).
                event(SageIdentify.class, (event,state) -> handleIdentify(event))
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
//        onTransition(
//                matchState(null, States.Idle, (from,to) -> IdentifyShards(self(), context()))
//        );

//        onTransition(
//                matchState(null, States.Idle, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }

    private State handleShard(SageIdentity event, State state, boolean unstash) throws Exception {
        ++state.countShards;
        if (unstash) unstashAll();
        context().watch(event.getRef());
        log().info("Shard detected");
        return state;
    }



    private ActorRef IdentifyShards()
    {
        ActorRef shards = context().actorOf(FromConfig.getInstance().props(), SHARDS_NAME);
        context().watch(shards);
        shards.tell(new SageIdentify(1), self());
        return shards;
    }


    private FSM.State<States, State> handleIdentify(SageIdentify event) {
        context().watch(sender());
        return stay().replying(SageIdentity.from(event, self()));
    }

    private FSM.State<States, State> launchRequest(StartCalcMultiRic event, State state)
    {
        String name = event.toActorName(state.idNext++);

        // create at the root - not as children so Termination of calc doesn't terminate the assembler
        ActorRef calcAsm = context().system().actorOf(CalcAssembler.props(event, sender(), state.countShards), name);
        state.shards.tell(event, calcAsm); // calcAsm must be the receiver - not the assembler itsef !
        return stay();
    }

}
