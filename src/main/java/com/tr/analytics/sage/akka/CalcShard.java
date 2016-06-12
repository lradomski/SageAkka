package com.tr.analytics.sage.akka;

import akka.actor.*;
import akka.japi.Creator;
import com.tr.analytics.sage.akka.data.*;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CalcShard extends AbstractFSMWithStash<CalcShard.States, CalcShard.State>
{
    public static enum States { Init, Ready };

    public static final class State
    {
        int idNext = 0;
        private final LinkedList<ActorRef> childCalcs = new LinkedList<>();

    }

    final ActorRef calcAsm;
    final StartCalcMultiRic req;
    final ExecutionContext longCalcDispatcher;

    public CalcShard(StartCalcMultiRic req, ActorRef calcAsm, ExecutionContext longCalcDispatcher)
    {
        this.req = req;
        this.calcAsm = calcAsm;
        this.longCalcDispatcher = longCalcDispatcher;
    }

    public static Props props(final StartCalcMultiRic req, final ActorRef client, ExecutionContext longCalcDispatcher) {
        return Props.create(CalcShard.class,(Creator<CalcShard>) () -> new CalcShard(req, client, longCalcDispatcher));
    }

    @Override
    public void preStart() throws Exception {
        context().watch(calcAsm);
        super.preStart();
    }


    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }


    public static final FiniteDuration INIT_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);

    {
        startWith(States.Init, new State(), INIT_TIMEOUT);

        when(States.Init,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Initialization timeout."))).
                event(Terminated.class, (event, state) -> stop(new Failure("Child or parent calc stopped."), state)).
                event(TradeRouter.RicStoreRefs.class, (event, state) -> launchRicRequestsGoTo(event, state, States.Ready))
        );

        when(States.Ready,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure("Child or parent calc stopped."))).
                event(CalcResultCore.class, (event, state) -> handleResult(event,state)).
                event(CalcUpdateCore.class, (event, state) -> handleUpdate(event,state))
        );

        whenUnhandled(
                matchAnyEvent((event, state) -> {
                    log().warning("Calc received unhandled event {} in state {}/{}", event, stateName(), state);
                    return stay();
                })
        );

        // logging
        onTransition(
                matchState(null, null, (from,to) -> log().debug("From: " + from.toString() + ", to: " + to.toString() + ", data: " + stateData()))
        );

        // init

//        onTransition(
//                matchState(null, States.Idle, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }


    private FSM.State<States, State> launchRicRequestsGoTo(TradeRouter.RicStoreRefs event, State state, States newState)
    {
        calcAsm.tell(event, self()); // ... so it can do its bookkeeping whether it got all rics from everywhere

        for (TradeRouter.RicStoreRefs.RicStoreRef ricRef : event.getRicRefs())
        {
            int id = state.idNext++;
            StartCalcSingleRic reqSingleRic = StartCalcSingleRic.fromFor(req, id, ricRef.getRic());

            ActorRef calcRic = context().actorOf(CalcRic.props(self(), reqSingleRic, ricRef.getRicStore(), longCalcDispatcher), ricRef.getRic());
            // ... is watched automatically as child (and its reference to this object is context().parent()

            state.childCalcs.add(calcRic); // keep track here to distinguish from other children - if any ...

            ricRef.getRicStore().tell(reqSingleRic, calcRic); // set calcRic as sender !
        }

        return goTo(newState);
    }

    private FSM.State<States, State> handleResult(CalcResultCore event, State state)
    {
        calcAsm.tell(event, self()); // TODO: real handling
        return stay();
    }


    private FSM.State<States, State> handleUpdate(CalcUpdateCore event, State state)
    {
        calcAsm.tell(event, self()); // TODO: real handling
        return stay();
    }
}
