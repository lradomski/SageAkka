package com.tr.analytics.sage.akka;

import akka.actor.*;
import akka.japi.Creator;
import akka.japi.function.Function3;
import com.tr.analytics.sage.akka.data.*;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CalcShard extends AbstractFSMWithStash<CalcShard.States, CalcShard.State>
{
    public static enum States {WaitForRicStores, SendCalc, SendCalcWaitAllResp, WaitForAllResp};

    public static final class State
    {
        private final LinkedList<TradeRouter.RicStoreRefs.RicActorRef> calcRics = new LinkedList<>();

        // full result
        TradeTotals totals = new TradeTotals();

        // partial results kept in init stages only
        final HashMap<Integer, TradeTotals> ricTotals = new HashMap<>();

    }

    final ActorRef calcAsm;
    final StartCalcMultiRic req;
    final ExecutionContext longCalcDispatcher;
    final Function3<ActorRefFactory, Props, String, ActorRef> calcRicMaker;

    public CalcShard(StartCalcMultiRic req, ActorRef calcAsm, ExecutionContext longCalcDispatcher)
    {
        this(req, calcAsm, longCalcDispatcher, (factory, props, name) -> factory.actorOf(props, name));
    }

    public CalcShard(
            StartCalcMultiRic req, ActorRef calcAsm, ExecutionContext longCalcDispatcher,
            Function3<ActorRefFactory, Props, String, ActorRef> calcRicMaker
    ) {
        this.calcAsm = calcAsm;
        this.req = req;
        this.longCalcDispatcher = longCalcDispatcher;
        this.calcRicMaker = calcRicMaker;
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
    public static final FiniteDuration ALL_RESP_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);

    public static final String DEPENDENCY_TERMINATION_MESSAGE = "CalcShard or CalcRich stopped.";

    {
        startWith(States.WaitForRicStores, new State(), INIT_TIMEOUT);

        when(States.WaitForRicStores,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for RicStores."))).
                event(Terminated.class, (event, state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE), state)).
                event(TradeRouter.RicStoreRefs.class, (event, state) -> launchRicRequestsGoTo(event, state, States.WaitForAllResp)
                        .forMax(ALL_RESP_TIMEOUT))
        );

        when(States.WaitForAllResp,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for RicStores."))).
                event(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> ifHaveAllSendGoTo(event, state, States.SendCalc)).
                event(CalcUpdateCore.class, this::updatePartialResultDontSendStay)
        );

        when(States.SendCalc,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> sendRefreshToOtherGoTo(event, state, States.SendCalcWaitAllResp)).
                event(CalcUpdateCore.class, this::updateResultSendStay)
        );

        when(States.SendCalcWaitAllResp,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> ifHaveAllSendGoTo(event, state, States.SendCalc)).
                event(CalcUpdateCore.class, this::updatePartialAndResultSendStay)
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

    private FSM.State<States, State> launchRicRequestsGoTo(TradeRouter.RicStoreRefs event, State state, States newState) throws Exception
    {
        calcAsm.tell(event, self()); // ... so it can do its bookkeeping whether it got all rics from everywhere

        int idRic = 0;
        for (TradeRouter.RicStoreRefs.RicActorRef ricRef : event.getRicRefs())
        {
            StartCalcSingleRic reqSingleRic = StartCalcSingleRic.fromFor(req, idRic++, ricRef.getRic());

            ActorRef calcRic = makeCalcRic(ricRef, reqSingleRic);
            context().watch(calcRic); // for testing/injection cases

            state.calcRics.add(new TradeRouter.RicStoreRefs.RicActorRef(ricRef.getRic(), calcRic)); // keep track here to distinguish from other children - if any ...

            ricRef.getRicStore().tell(reqSingleRic, calcRic); // set calcRic as sender !
        }

        return goTo(newState);
    }

    private ActorRef makeCalcRic(TradeRouter.RicStoreRefs.RicActorRef ricRef, StartCalcSingleRic reqSingleRic) throws Exception {
        ActorRefFactory factory = context();
        Props props = CalcRic.props(self(), reqSingleRic, ricRef.getRicStore(), longCalcDispatcher);
        return calcRicMaker.apply(factory, props, ricRef.getRic());
    }

    // returns true if more responses needed still (in order to gather all)
    private boolean applResponseToPartialCheckHasAll(CalcResultCore event, State state)
    {
        CalcResult<TradeTotals> tt = (CalcResult<TradeTotals>)event;

        state.ricTotals.put(tt.getId(), tt.getData());

        if (state.ricTotals.size() == state.calcRics.size())
        {
            for(TradeTotals ricTotal : state.ricTotals.values())
            {
                state.totals = state.totals.makeUpdated(ricTotal);
            }
            state.ricTotals.clear(); // clear partial state



            return true;

        }
        else
        {
            return false;

        }
    }

    private FSM.State<States, State> ifHaveAllSendGoTo(CalcResultCore event, State state, States nextState)
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

    private FSM.State<States, State> updatePartialResultDontSendStay(CalcUpdateCore event, State state)
    {
        CalcUpdate<TradeTotals> u = (CalcUpdate<TradeTotals>)event;
        updatePartialState(u, state);
        return stay();
    }

    private FSM.State<States, State> updateResultSendStay(CalcUpdateCore event, State state)
    {
        updateSendResult((CalcUpdate<TradeTotals>) event, state);

        return stay();
    }

    private FSM.State<States, State> updatePartialAndResultSendStay(CalcUpdateCore event, State state)
    {
        CalcUpdate<TradeTotals> u = (CalcUpdate<TradeTotals>) event;
        updateSendResult(u, state);
        updatePartialState(u, state);

        return stay();
    }

    private FSM.State<States, State> sendRefreshToOtherGoTo(CalcResultCore event, State state, States newState) {

        if (applResponseToPartialCheckHasAll(event, state))
        {
            sendResult(state);
            return stay();
        }
        else
        {

            for (int idRic = 0; idRic < state.calcRics.size(); idRic++)
            {
                TradeRouter.RicStoreRefs.RicActorRef ricRef = state.calcRics.get(idRic);
                if (idRic != event.getId()) // skip the one who just refreshed
                {

                    StartCalcSingleRic reqSingleRic = StartCalcSingleRic.fromFor(req, idRic++, ricRef.getRic());
                    ricRef.getRicStore().tell(reqSingleRic, self());
                }
            }


            return goTo(newState);
        }
    }




    private void updatePartialState(CalcUpdate<TradeTotals> event, State state) {
        TradeTotals ricTotals = state.ricTotals.get(event.getId());
        if (null != ricTotals) {
            state.ricTotals.put(event.getId(), ricTotals.makeUpdated(event.getData()));
        }
    }

    private void updateSendResult(CalcUpdate<TradeTotals> event, State state) {
        CalcUpdate<TradeTotals> u = event;
        state.totals = state.totals.makeUpdated(u.getData());
        sendUpdate(u);
    }

    private void sendResult(State state) {
        calcAsm.tell(new CalcResult<TradeTotals>(req.getId(), state.totals), self());
    }

    private void sendUpdate(CalcUpdate<TradeTotals> u) {
        calcAsm.tell(new CalcUpdate<TradeTotals>(req.getId(), u.getData()), self());
    }

}
