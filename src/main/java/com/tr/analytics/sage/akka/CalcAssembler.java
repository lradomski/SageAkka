package com.tr.analytics.sage.akka;

import akka.actor.*;
import akka.japi.Creator;
import com.tr.analytics.sage.akka.data.*;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CalcAssembler extends CalcReduceBase<CalcAssembler.States, CalcAssembler.Data>
{
    public static enum States {WaitForAllRics, WaitForAllResp, SendCalc, SendCalcWaitAllRefresh };

    public static final class Data
    {
        final LinkedList<ActorRef> calcShards = new LinkedList<>();
        Set<String> ricsNotAccounted = null;

        void ensureRicsNotAccounted(StartCalcMultiRic req)
        {
            if (null == ricsNotAccounted) {
                ricsNotAccounted  = new HashSet<>();
                for (String ric : req.getRics()) {
                    if (!ricsNotAccounted.contains(ric)) {
                        // TODO: handle duplicate rics
                    }

                    ricsNotAccounted.add(ric);
                }
            }
        }

        boolean accountRics(TradeRouter.RicStoreRefs ricRefs)
        {
            for (TradeRouter.RicStoreRefs.RicActorRef ricRef : ricRefs.getRicRefs())
            {
                ricsNotAccounted.remove(ricRef.getRic());
            }

            return ricsNotAccounted.isEmpty();
        }
    }


    final ActorRef client;
    final int countShards;

    public CalcAssembler(StartCalcMultiRic req, ActorRef client, int countShards)
    {
        super(req, client);
        this.client = client;
        this.countShards = countShards;
    }

    public static Props props(final StartCalcMultiRic req, final ActorRef client, final int countShards) {
        return Props.create(CalcAssembler.class,(Creator<CalcAssembler>) () -> new CalcAssembler(req, client, countShards));
    }

    @Override
    public void preStart() throws Exception {
        context().watch(client);
        super.preStart();
    }

    @Override
    protected void sendRefreshRequests(CalcResultCore event, State<Data> state) {
        for (ActorRef calcShard : state.data.calcShards)
        {
            // TODO: refresh requests
        }

    }

    public static final FiniteDuration INIT_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);
    public static final FiniteDuration ALL_RESP_TIMEOUT = Duration.create(3*60, TimeUnit.SECONDS);

    public static final String DEPENDENCY_TERMINATION_MESSAGE = "SimpleClient or CalcShard terminated.";

    {
        startWith(States.WaitForAllRics, new State(new Data()), INIT_TIMEOUT);

        when(States.WaitForAllRics,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for ric responses."))).
                event(Terminated.class, (event, state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE), state)).
                event(TradeRouter.RicStoreRefs.class,(event,state)-> accountRicsTryUnstashGoTo(event,state,States.WaitForAllResp)
                        .forMax(ALL_RESP_TIMEOUT)).
                event(CalcResultCore.class, (event, state) -> { stash(); return stay();}).
                event(CalcUpdateCore.class, (event, state) -> { stash(); return stay();})
        );

        when(States.WaitForAllResp,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout waiting for RicStores."))).
                event(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> ifHaveAllSendGoToOrStop(event, state, States.SendCalc)).
                event(CalcUpdateCore.class, this::updatePartialResultDontSendStay)
        );

        when(States.SendCalc,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                event(CalcResultCore.class, (event,state) -> sendRefreshToOtherGoTo(event, state, States.SendCalcWaitAllRefresh)).
                event(CalcUpdateCore.class, (event,state) -> updateResultSendStay(event, state))
        );

        when(States.SendCalcWaitAllRefresh,
                matchEvent(Terminated.class, (event,state) -> stop(new Failure(DEPENDENCY_TERMINATION_MESSAGE))).
                        event(CalcResultCore.class, (event,state) -> ifHaveAllSendGoTo(event, state, States.SendCalc)).
                        event(CalcUpdateCore.class, (event,state) -> updatePartialAndResultSendStay(event,state))
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

    private FSM.State<States,State<Data>> ifHaveAllSendGoToOrStop(CalcResultCore event, State<Data> state, States newState) {
        if (ifHaveAllSend(event, state))
        {
            return req.isSnapshot() ? stop(Normal()) : goTo(newState);
        }
        else
        {
            return stay();
        }
    }

    private FSM.State<CalcAssembler.States,CalcReduceBase.State<CalcAssembler.Data>>
    accountRicsTryUnstashGoTo(TradeRouter.RicStoreRefs event, CalcReduceBase.State<CalcAssembler.Data> state, States nextState)
    {
        state.data.calcShards.add(sender());
        context().watch(sender());

        state.countRespondents = this.countShards;
        state.data.ensureRicsNotAccounted(this.req);

        if (req.isAllRics() || state.data.accountRics(event))
        {
            unstashAll();
            return goTo(nextState);
        }
        else
        {
            if (this.countShards == state.data.calcShards.size())
            {
                // got responses from all calcShards
                return stop(new Failure("Some rics not found any any shard: " + state.data.ricsNotAccounted));
            }
            else
            {
                // still some response to come
                return stay();
            }
        }
    }

    private FSM.State<CalcAssembler.States,CalcReduceBase.State<CalcAssembler.Data>> launchRequest(StartCalcMultiRic event, State state)
    {
 //       state.shards.tell(event, sender());
        return stay();
    }

    private FSM.State<CalcAssembler.States,CalcReduceBase.State<CalcAssembler.Data>> buildPartialResultDontSendStay(CalcResultCore event, State state)
    {
        applyResponseToPartialCheckHasAll(event, state);
        return stay();
    }

}
