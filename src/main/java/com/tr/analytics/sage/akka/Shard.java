package com.tr.analytics.sage.akka;


import akka.japi.Creator;
import com.tr.analytics.sage.akka.data.*;
import com.tr.analytics.sage.shard.engine.TradeReal;

import akka.actor.*;
import akka.routing.FromConfig;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class Shard extends AbstractFSMWithStash<Shard.States, Shard.State> {

    private ActorRef tradeRouter = null;


    public static final String NAME = "shard";
    public static enum States { Init, Ready };

    public static final class State
    {
        boolean streamingStarted = false;

        int idNext = 0;
        final ActorRef sources;

        public State(ActorRef sources)
        {
            this.sources = sources;
        }
    }

    final ExecutionContext longCalcDispatcher;

    public Shard(ExecutionContext longCalcDispatcher)
    {

        this.longCalcDispatcher = longCalcDispatcher;
    }

    public static Props props(ExecutionContext longCalcDispatcher) {
        return Props.create(Shard.class,(Creator<Shard>) () -> new Shard(longCalcDispatcher));
    }


    @Override
    public void preStart() throws Exception {
        tradeRouter = context().system().actorOf(Props.create(TradeRouter.class), TradeRouter.NAME);
        context().watch(tradeRouter);
        super.preStart();
    }



    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public static final String LONG_CALC_DISPATCHER_NAME = "dispatcher-long-calc";
    public static final FiniteDuration INIT_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);

    {
        startWith(States.Init, new State(IdentifySources()), INIT_TIMEOUT);

        when(States.Init,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Shard initialization timeout."))).
                event(SageIdentity.class, (event, state) -> goTo(States.Ready).using(handleTradeSource(event, state, true))).
                event(SageIdentify.class, (event,state) -> handleIdentify(event)).
                event(Terminated.class, (event,state) -> stop(new Failure("Trade Source stopped."), state)).
                event(StartCalc.class, (event,state) -> { stash(); return stay();   })
        );

        when(States.Ready,
                matchEvent(Terminated.class, (event,state) -> stay()). //TODO: uncomment: //stop(new Failure("Shard stopped."))).
                event(SageIdentity.class, (event, state) -> stay().using(handleTradeSource(event, state, false))).
                event(StartCalcMultiRic.class, (event, state) -> {
                    log().debug(event.toString());
                    if (event.getCalcName().equals("start"))
                    {
                        if (!state.streamingStarted) {
                            state.sources.tell("start", self()); // TODO: permanent replay ?
                            state.streamingStarted = true;
                        }
                        return stay();
                    }
                    return launchRequest(event, state).replying(CalcResultCore.from(event)); // TOD: remove "replying"

                }).
                event(TradeReal.class, (event, state) ->
                {
                    tradeRouter.tell(event, sender());
//                    String s = event.toString();
//                    System.out.println(s);
//                    log().debug(s);
                    return stay();
                } ).
                event(SageIdentify.class, (event,state) -> handleIdentify(event))
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
//        onTransition(
//                matchState(null, States.Idle, (from,to) -> IdentifySources(self(), context()))
//        );

//        onTransition(
//                matchState(null, States.Idle, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();
    }

    private FSM.State<States, State> handleIdentify(SageIdentify event) {
        context().watch(sender());
        return stay().replying(SageIdentity.from(event, self()));
    }

    private State handleTradeSource(SageIdentity event, State state, boolean unstash) throws Exception {
        if (unstash) unstashAll();
        context().watch(event.getRef());
        log().info("Trade Source detected");
        return state;
    }

    private FSM.State<States, State> launchRequest(StartCalcMultiRic event, State state)
    {
        String name = event.toActorName(state.idNext++);

        // create in global context so calcShard failure doesn't terminate Shard.
        // Hook up calcShard to its sender (calcAsm).
        ActorRef calcShard = context().system().actorOf(CalcShard.props(event, sender(), longCalcDispatcher), name);
        tradeRouter.tell(event, calcShard); // ask for references to RicStores to be routed to calcShard
        return stay();
    }

    private ActorRef IdentifySources() {
        ActorRef sources = context().system().actorOf(FromConfig.getInstance().props(), "trade-sources");
        context().watch(sources);
        sources.tell(new SageIdentify(1), self());
        return sources;
    }
}
