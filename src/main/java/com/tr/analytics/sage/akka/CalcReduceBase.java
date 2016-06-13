package com.tr.analytics.sage.akka;

import akka.actor.*;
import com.tr.analytics.sage.akka.data.*;
import scala.concurrent.duration.Duration;

import java.util.HashMap;

public abstract class CalcReduceBase<States, Data> extends AbstractFSMWithStash<States, CalcReduceBase.State<Data>>
{
    public static final class State<Data>
    {
        final Data data;
        int countRespondents = 0;

        // full result
        TradeTotals totals = new TradeTotals();

        // partial results kept in init stages only and when refreshing
        final HashMap<Integer, TradeTotals> partialTotals = new HashMap<>();

        public State(Data data)
        {
            this.data = data;

        }

    }

    final protected ActorRef target;
    final StartCalcMultiRic req;

    public CalcReduceBase(StartCalcMultiRic req, ActorRef target)
    {
        this.req = req;
        this.target = target;

    }

//    public static Props props(final StartCalcMultiRic req, final ActorRef client) {
//        return Props.create(CalcReduceBase.class,(Creator<CalcReduceBase>) () -> new CalcReduceBase(req, client));
//    }

    @Override
    public void preStart() throws Exception {
        context().watch(target);
        super.preStart();
    }


    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }



    // returns true if more responses needed still (in order to gather all)
    protected boolean applResponseToPartialCheckHasAll(CalcResultCore event, State<Data> state)
    {
        CalcResult<TradeTotals> tt = (CalcResult<TradeTotals>)event;

        state.partialTotals.put(tt.getId(), tt.getData());

        if (state.partialTotals.size() == state.countRespondents)
        {
            for(Object oRicTotal : state.partialTotals.values())
            {
                TradeTotals ricTotal = (TradeTotals)oRicTotal;
                state.totals = state.totals.makeUpdated(ricTotal);
            }
            state.partialTotals.clear(); // clear partial state
            return true;
        }
        else
        {
            return false;

        }
    }

    protected FSM.State<States, CalcReduceBase.State<Data>> ifHaveAllSendGoTo(CalcResultCore event, CalcReduceBase.State<Data> state, States nextState)
    {
        if (applResponseToPartialCheckHasAll(event, state))
        {
            sendResult(state);
            return goTo(nextState);
        }
        else
        {
            return stay();
        }
    }

    protected FSM.State<States, CalcReduceBase.State<Data>> updatePartialResultDontSendStay(CalcUpdateCore event, State<Data> state)
    {
        CalcUpdate<TradeTotals> u = (CalcUpdate<TradeTotals>)event;
        updatePartialState(u, state);
        return stay();
    }

    protected FSM.State<States, CalcReduceBase.State<Data>> updateResultSendStay(CalcUpdateCore event, State<Data> state)
    {
        updateSendResult((CalcUpdate<TradeTotals>) event, state);

        return stay();
    }

    protected FSM.State<States, CalcReduceBase.State<Data>> updatePartialAndResultSendStay(CalcUpdateCore event, State<Data> state)
    {
        CalcUpdate<TradeTotals> u = (CalcUpdate<TradeTotals>) event;
        updateSendResult(u, state);
        updatePartialState(u, state);

        return stay();
    }

    protected FSM.State<States, CalcReduceBase.State<Data>> sendRefreshToOtherGoTo(CalcResultCore event, CalcReduceBase.State<Data> state, States newState) {

        if (applResponseToPartialCheckHasAll(event, state))
        {
            sendResult(state);
            return stay();
        }
        else
        {

            sendRefreshRequests(event, state);


            return goTo(newState);
        }
    }

    protected abstract void sendRefreshRequests(CalcResultCore event, State<Data> state);


    protected void updatePartialState(CalcUpdate<TradeTotals> event, State<Data> state) {
        TradeTotals ricTotals = state.partialTotals.get(event.getId());
        if (null != ricTotals) {
            state.partialTotals.put(event.getId(), ricTotals.makeUpdated(event.getData()));
        }
    }

    protected void updateSendResult(CalcUpdate<TradeTotals> event, State<Data> state) {
        CalcUpdate<TradeTotals> u = event;
        state.totals = state.totals.makeUpdated(u.getData());
        sendUpdate(u);
    }

    protected void sendResult(State state) {
        target.tell(new CalcResult<TradeTotals>(req.getId(), state.totals), self());
    }

    protected void sendUpdate(CalcUpdate<TradeTotals> u) {
        target.tell(new CalcUpdate<TradeTotals>(req.getId(), u.getData()), self());
    }

}
