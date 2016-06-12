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

    public static enum States {WaitForResp, WaitForRespCalc, SendingCalc};

    public static final class State
    {
        TradeTotals totals = new TradeTotals();
        int idPendingCalc = 0;
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

    public static final FiniteDuration INIT_TIMEOUT = Duration.create(3, TimeUnit.SECONDS);

    {
        startWith(States.WaitForResp, new State(), INIT_TIMEOUT);

        when(States.WaitForResp,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Initialization timeout."))).
                event(CalcResultCore.class, (event, state) -> launchRespCalcGoTo(event, state, States.WaitForRespCalc)).
                event(Terminated.class, (event, state) -> stop(new Failure("RicStore or CalcShard stopped."), state))
        );

        when(States.WaitForRespCalc,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure("RicStore or CalcShard stopped."))).
                event(CalcResultCore.class, (event, state) -> launchRespCalcGoTo(event, state, States.WaitForRespCalc)).
                event(CalcUpdateCore.class, (event, state) -> stashUpdateStay(event, state))
        );

        whenUnhandled(
                matchAnyEvent((event, state) -> {
                    log().warning("Calc received unhandled event {} in state {}/{}",
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
        int idResponse = state.idPendingCalc++;

        Future<ResponseResult> calcResponse = future(
                () -> new ResponseResult(idResponse, TradeTotals.from(((CalcResult<RicStore.Trades>)event).getData())),
                longCalcDispatcher
        );
        Patterns.pipe(calcResponse, context().dispatcher()).to(self());

        return goTo(newState);
    }


    private FSM.State<States, State> stashUpdateStay(CalcUpdateCore event, State state)
    {
        stash();
        return stay();
    }

    private FSM.State<States, State> handleUpdate(CalcUpdateCore event, State state)
    {
        // TODO: real handling
        String tradeString = ((CalcUpdate<Trade>)event).getData().toString();
        CalcUpdate<String> result = new CalcUpdate<String>(event.getId(), "U/" + req.toString() + "/" + tradeString);

       calcShard.tell(event, self()); // TODO: real handling
        return stay();
    }
}
