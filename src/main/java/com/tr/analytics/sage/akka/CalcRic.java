package com.tr.analytics.sage.akka;

import akka.actor.*;
import akka.japi.Creator;
import com.tr.analytics.sage.akka.data.*;
import com.tr.analytics.sage.api.Trade;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class CalcRic extends AbstractFSMWithStash<CalcRic.States, CalcRic.State>
{
    public static Props props(StartCalcSingleRic req, ActorRef ricStore)
    {
        return Props.create(CalcRic.class,(Creator<CalcRic>) () -> new CalcRic(req, ricStore));
    }

    public static enum States { Init, Ready };

    public static final class State
    {
    }

    private final StartCalcSingleRic req;
    private final ActorRef ricStore;

    public CalcRic(StartCalcSingleRic req, ActorRef ricStore)
    {
        this.req = req;
        this.ricStore = ricStore;
    }


    @Override
    public void preStart() throws Exception {
        context().watch(ricStore);
        context().watch(context().parent()); // watch calcShard too
        super.preStart();
    }

    public static final FiniteDuration INIT_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);

    {
        startWith(States.Init, new State(), INIT_TIMEOUT);

        when(States.Init,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Initialization timeout."))).
                event(CalcResultCore.class, (event, state) -> stop(new Failure("Result in Init."), state)).
                event(Terminated.class, (event, state) -> stop(new Failure("RicStore or parent stopped."), state))
        );

        when(States.Ready,
                matchEvent(CalcResultCore.class, (event, state) -> stay()).
                event(CalcUpdateCore.class, (event, state) -> stay()).
                event(Terminated.class, (event,state) -> stop(new Failure("RicStore or parent stopped."), state))
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


    private FSM.State<States, State> handleResult(CalcResultCore event, State state)
    {
        // TODO: real handling
        int countTrades = ((CalcResult<RicStore.Trades>)event).getData().getCount();
        CalcResult<String> result = new CalcResult<String>(event.getId(), "R/" + req.toString() + "/" + Integer.toString(countTrades));

        context().parent().tell(result, self());
        return stay();
    }


    private FSM.State<States, State> handleUpdate(CalcUpdateCore event, State state)
    {
        // TODO: real handling
        String tradeString = ((CalcUpdate<Trade>)event).getData().toString();
        CalcUpdate<String> result = new CalcUpdate<String>(event.getId(), "U/" + req.toString() + "/" + tradeString);

        context().parent().tell(event, self()); // TODO: real handling
        return stay();
    }
}
