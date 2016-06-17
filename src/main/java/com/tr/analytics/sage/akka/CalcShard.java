package com.tr.analytics.sage.akka;

import akka.actor.*;
import akka.japi.Creator;
import akka.japi.function.Function3;
import com.tr.analytics.sage.akka.data.*;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CalcShard extends CalcReduceBase<CalcShard.States, CalcShard.Data>
{
    public static enum States {WaitForRicStores, WaitForAllResp, SendCalc, SendCalcWaitAllRefresh, WaitEnd};

    public static final class Data
    {
        final LinkedList<TradeRouter.RicStoreRefs.RicActorRef> calcRics = new LinkedList<>();
    }

    final ActorRef calcAsm;
    final ExecutionContext longCalcDispatcher;
    final Function3<ActorRefFactory, Props, String, ActorRef> calcRicMaker;

    public CalcShard(StartCalcMultiRic req, ActorRef calcAsm, ExecutionContext longCalcDispatcher)
    {
        this(req, calcAsm, longCalcDispatcher, (factory, props, name) -> factory.actorOf(props, name));
    }

    public CalcShard(
            StartCalcMultiRic req, ActorRef calcAsm, ExecutionContext longCalcDispatcher,
            Function3<ActorRefFactory, Props, String, ActorRef> calcRicMaker
    )
    {
        super(req, calcAsm);
        this.calcAsm = calcAsm;
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

    @Override
    protected void sendRefreshRequests(CalcResultCore event, State<Data> state) {
        for (int idRic = 0; idRic < state.data.calcRics.size(); idRic++)
        {
            TradeRouter.RicStoreRefs.RicActorRef ricRef = state.data.calcRics.get(idRic);
            if (idRic != event.getId()) // skip the one who just refreshed
            {

                StartCalcSingleRic reqSingleRic = StartCalcSingleRic.fromFor(req, idRic++, ricRef.getRic());
                ricRef.getRicStore().tell(reqSingleRic, self());
            }
        }

    }

    public static final FiniteDuration INIT_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);
    public static final FiniteDuration ALL_RESP_TIMEOUT = Duration.create(3*60, TimeUnit.SECONDS);

    public static final String DEPENDENCY_TERMINATION_MESSAGE = "CalcShard or CalcRic stopped.";

    {
        startWith(States.WaitForRicStores, new State<Data>(new Data()), INIT_TIMEOUT);

        when(States.WaitForRicStores,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for RicStores."))).
                event(Terminated.class, (event, state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE), state)).
                event(TradeRouter.RicStoreRefs.class, (event, state) -> launchRicRequestsGoTo(event, state, States.WaitForAllResp)
                        .forMax(ALL_RESP_TIMEOUT))
        );

        when(States.WaitForAllResp,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for RicStores."))).
                event(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> ifHaveAllSendGoTo(event, state, state.req.isSnapshot() ? States.WaitEnd : States.SendCalc)).
                event(CalcUpdateCore.class, this::updatePartialResultDontSendStay)
        );

        when(States.SendCalc,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> sendRefreshToOtherGoTo(event, state, States.SendCalcWaitAllRefresh)).
                event(CalcUpdateCore.class, this::updateResultSendStay)
        );

        when(States.SendCalcWaitAllRefresh,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> ifHaveAllSendGoTo(event, state, States.SendCalc)).
                event(CalcUpdateCore.class, this::updatePartialAndResultSendStay)
        );

        // Use in case req.isSnapshot after we've gotten a response.
        // We can't terminate because it would terminate entire network of actors for this request - perhaps before
        // request is served. CalcAsm terminates itself after sending out response
        when(States.WaitEnd,
                matchEvent(Terminated.class, (event,state) -> stop(Normal())).
                anyEvent((event,state) -> stay())
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

    private FSM.State<CalcShard.States, CalcReduceBase.State<CalcShard.Data>> launchRicRequestsGoTo(TradeRouter.RicStoreRefs event, State<Data> state, CalcShard.States newState) throws Exception
    {
        state.req = req;
        if (req.isAllRics())
        {
            // for "all rics" calcAsm just needs to get anything as it actually doesn't do any accounting.
            // either way Asm should probably know all the rics on each shard without having to find out at request time ..
            // TODO: above line
            calcAsm.tell(new TradeRouter.RicStoreRefs(new LinkedList<TradeRouter.RicStoreRefs.RicActorRef>()), self());
        }
        else
        {
            calcAsm.tell(event, self()); // ... so it can do its bookkeeping whether it got all rics from everywhere
        }

        int idRic = 0;
        for (TradeRouter.RicStoreRefs.RicActorRef ricRef : event.getRicRefs())
        {
            StartCalcSingleRic reqSingleRic = StartCalcSingleRic.fromFor(req, idRic++, ricRef.getRic());

            ActorRef calcRic = makeCalcRic(ricRef, reqSingleRic);
            context().watch(calcRic); // for testing/injection cases

            state.data.calcRics.add(new TradeRouter.RicStoreRefs.RicActorRef(ricRef.getRic(), calcRic)); // keep track here to distinguish from other children - if any ...

            ricRef.getRicStore().tell(reqSingleRic, calcRic); // set calcRic as sender !
        }

        state.countRespondents = idRic;
        //System.out.println(">>> " + Integer.toString(state.countRespondents));

        return goTo(newState);
    }

    private ActorRef makeCalcRic(TradeRouter.RicStoreRefs.RicActorRef ricRef, StartCalcSingleRic reqSingleRic) throws Exception {
        ActorRefFactory factory = context();
        Props props = CalcRic.props(self(), reqSingleRic, ricRef.getRicStore(), longCalcDispatcher);
        return calcRicMaker.apply(factory, props, ricRef.getRic());
    }


}
