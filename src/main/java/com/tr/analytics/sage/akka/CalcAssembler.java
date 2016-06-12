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

public class CalcAssembler extends AbstractFSMWithStash<CalcAssembler.States, CalcAssembler.State>
{
    public static enum States { Init, Ready };

    public static final class State
    {
        private final LinkedList<ActorRef> calcShards = new LinkedList<>();
        private Set<String> ricsNotAccounted = null;

        public State()
        {}

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
    final StartCalcMultiRic req;

    public CalcAssembler(StartCalcMultiRic req, ActorRef client, int countShards)
    {
        this.req = req;
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

    public static final FiniteDuration INIT_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);

    {
        startWith(States.Init, new State(), INIT_TIMEOUT);

        when(States.Init,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Initialization timeout."))).
                event(TradeRouter.RicStoreRefs.class, (event,state) -> accountRicsTryGoTo(event, state, States.Ready)).
                event(Terminated.class, (event, state) -> stop(new Failure("Child calc or client stopped."), state)).
                event(CalcResultCore.class, (event, state) -> handleResult(event, state)). // TODO: remove
                event(CalcUpdateCore.class, (event, state) -> handleUpdate(event, state)) // TODO: remove
        );

        when(States.Ready,
                matchEvent(StartCalcMultiRic.class, (event, state) -> launchRequest(event, state)).

                // Ignore it since we must've gotted rics calcShards whic already responded.
                // We won't keep track of that calcShard and if rics are sharded exclusively between shards
                // it won't be doing any work anyway (it doesn't have any rics)
                event(TradeRouter.RicStoreRefs.class, (event,state) -> stay()).
                event(Terminated.class, (event,state) -> stop(new Failure("hild calc or client stopped."), state)).
                event(CalcResultCore.class, (event, state) -> handleResult(event, state)). // TODO: remove
                event(CalcUpdateCore.class, (event, state) -> handleUpdate(event, state)) // TODO: remove
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

    private FSM.State<States,State> accountRicsTryGoTo(TradeRouter.RicStoreRefs event, State state, States nextState) {
        state.calcShards.add(sender());
        context().watch(sender());

        state.ensureRicsNotAccounted(this.req);

        if (state.accountRics(event))
        {
            // maybe we didn't get all responses yet, but we got all the rics - so proceed
            return goTo(nextState);
        }
        else
        {
            if (this.countShards == state.calcShards.size())
            {
                // got responses from all calcShards
                return stop(new Failure("Some rics not found any any shard: " + state.ricsNotAccounted));
            }
            else
            {
                // still some response to come
                return stay();
            }
        }
    }

    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }


    private FSM.State<States, State> launchRequest(StartCalcMultiRic event, State state)
    {
 //       state.shards.tell(event, sender());
        return stay();
    }

    private FSM.State<States, State> handleResult(CalcResultCore event, State state)
    {
        client.tell(event, self()); // TODO: real handling
        return stay();
    }


    private FSM.State<States, State> handleUpdate(CalcUpdateCore event, State state)
    {
        client.tell(event, self()); // TODO: real handling
        return stay();
    }
}
