package com.tr.analytics.sage.akka;

import akka.actor.*;
import akka.japi.Creator;
import akka.pattern.Patterns;
import com.tr.analytics.sage.akka.data.*;
import com.tr.analytics.sage.api.Trade;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static akka.dispatch.Futures.future;

public class CalcRic extends AbstractFSMWithStash<CalcRic.States, CalcRic.State>
{
    public static Props props(ActorRef calcShard, StartCalcSingleRic req, ActorRef ricStore, ExecutionContext longCalcDispatcher)
    {
        return Props.create(CalcRic.class,(Creator<CalcRic>) () -> new CalcRic(calcShard, req, ricStore, longCalcDispatcher));
    }

    public static enum States {WaitForResp, WaitForRespCalc, SendCalcWaitForResp, SendCalcWaitForRespCalc, SendCalc};

    public static final class State
    {
        TradeTotals totals = new TradeTotals();
        int idPendingCalc = -1;
    }

    public static class Refresh extends StartCalc
    {
        public Refresh(String calcName, String instanceName, int id) {
            super(calcName, instanceName, id);
        }
    }

    protected static final class ResponseResult
    {
        final int id;
        final TradeTotals result;

        public ResponseResult(int id, TradeTotals result) {
            this.id = id;
            this.result = result;
        }
    }

    private final ActorRef calcShard;
    private final StartCalcSingleRic req;
    private final ActorRef ricStore;
    private final ExecutionContext longCalcDispatcher;

    public CalcRic(ActorRef calcShard, StartCalcSingleRic req, ActorRef ricStore, ExecutionContext longCalcDispatcher)
    {
        this.calcShard = calcShard;
        this.req = req;
        this.ricStore = ricStore;
        this.longCalcDispatcher = longCalcDispatcher;
    }


    @Override
    public void preStart() throws Exception {
        context().watch(ricStore);
        context().watch(calcShard);
        super.preStart();
    }

    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }


    public static final FiniteDuration INIT_TIMEOUT = Duration.create(3, TimeUnit.SECONDS);

    public static final String DEPENDENCY_TERMINATION_MESSAGE = "RicStore or CalcShard stopped.";

    private static final Duration RESPONSE_CALC_TIMEOUT = Duration.create(3, TimeUnit.SECONDS);

    {
        startWith(States.WaitForResp, new State(), INIT_TIMEOUT);

        when(States.WaitForResp,
                matchEvent(Terminated.class, (event, state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                eventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for response."))).
                event(Refresh.class, (event,state) -> stay()).
                event(CalcResultCore.class, (event, state) -> launchRespCalcGoTo(event, state, States.WaitForRespCalc).forMax(RESPONSE_CALC_TIMEOUT))
        );

        when(States.WaitForRespCalc,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                eventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for response calculation result."))).
                event(Refresh.class, (event,state) -> stay()).
                event(CalcResultCore.class, this::launchNewRespCalcStay).
                event(CalcUpdateCore.class, this::stashUpdateStay).
                event(ResponseResult.class, (event, state) -> ifValidSendUnstashGoTo(event, state, States.SendCalc))
        );

        when(States.SendCalc,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(Refresh.class, (event,state) -> stay().replying(result(state))).
                event(CalcResultCore.class, (event, state) -> launchRespCalcGoTo(event, state, States.SendCalcWaitForResp)).
                event(CalcUpdateCore.class, this::processSendUpdateStay)
        );

        when(States.SendCalcWaitForResp,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, this::launchNewRespCalcStay).
                event(CalcUpdateCore.class, this::stashProcessSendUpdateStay).
                event(ResponseResult.class, (event, state) -> ifValidSendUnstashGoTo(event, state, States.SendCalc))
        );
//
//        when(States.SendCalcWaitForRespCalc,
//            matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
//            event(CalcResultCore.class, this::launchNewRespCalcStay).
//            event(CalcUpdateCore.class, this::stashProcessSendUpdateStay).
//            event(ResponseResult.class, (event, state) -> ifValidSendUnstashGoTo(event, state, States.SendCalc))
//        );

        whenUnhandled(
                matchAnyEvent((event, state) -> {
                    log().warning("CalcRic received unhandled event {} in state {}/{}",
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
//                matchState(null, States.Idle, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }

    private FSM.State<States, State> launchRespCalcGoTo(CalcResultCore event, State state, States newState)
    {
        launchResp((CalcResult<RicStore.Trades>) event, state);
        return goTo(newState);
    }

    private void launchResp(CalcResult<RicStore.Trades> event, State state) {
        int idResponse = ++state.idPendingCalc;
        Future<ResponseResult> calcResponse = future(
                () -> new ResponseResult(idResponse, TradeTotals.from(event.getData())),
                longCalcDispatcher
        );
        Patterns.pipe(calcResponse, context().dispatcher()).to(self());
    }

    private FSM.State<States,State> launchNewRespCalcStay(CalcResultCore event, State state)
    {
        clearStash(); // clear old updates since we just got new response
        launchRespCalcGoTo(event, state, this.stateName()); // this will also generate new id to ignore old resp
        return stay();
    }

    private FSM.State<States, State> stashUpdateStay(CalcUpdateCore event, State state)
    {
        stash();
        return stay();
    }

    private FSM.State<States, State> ifValidSendUnstashGoTo(ResponseResult event, State state, States newState) {
        if (event.id == state.idPendingCalc)
        {
            state.totals = event.result;
            unstashAll();
            calcShard.tell(result(state), self());
            return goTo(newState);

        }
        else
        {
            return stay();
        }
    }

    private CalcResult<TradeTotals> result(State state) {
        return new CalcResult<>(req.getId(), state.totals);
    }


    private FSM.State<States, State> stashProcessSendUpdateStay(CalcUpdateCore event, State state)
    {
        stash(); // ... because it will be applied to pending response
        return processSendUpdateStay(event, state);
    }

    private FSM.State<States, State> processSendUpdateStay(CalcUpdateCore event, State state)
    {
        TradeTotals update = TradeTotals.from(((CalcUpdate<Trade>)event).getData());
        state.totals = state.totals.makeUpdated(update); // keep
        calcShard.tell(new CalcUpdate<>(req.getId(), update), self());
        return stay();
    }
}
