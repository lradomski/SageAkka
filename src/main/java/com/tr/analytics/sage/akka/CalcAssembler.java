package com.tr.analytics.sage.akka;

import akka.actor.*;
import akka.japi.Creator;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import com.tr.analytics.sage.akka.data.*;
import com.tr.analytics.sage.api.Trade;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class CalcAssembler extends AbstractFSMWithStash<CalcAssembler.States, CalcAssembler.State>
{
    public static enum States { Init, Ready };

    public static final class State
    {
        Router childCalcs = new Router(new BroadcastRoutingLogic());
        StartCalcMultiRic request;
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
        return Props.create((Creator<CalcAssembler>) () -> new CalcAssembler(req, client, countShards));
    }

    {
        startWith(States.Init, new State(), Duration.create(15, TimeUnit.SECONDS));

        when(States.Init,
                matchEventEquals(StateTimeout(), (event,state) -> stop(new Failure("Initialization timeout."))).
                event(Terminated.class, (event, state) -> stop(new Failure("Child calc stopped."), state))
        );

        when(States.Ready,
                matchEvent(StartCalcMultiRic.class, (event, state) -> launchRequest(event, state)).
                event(CalcResult.class, (event, state) -> stay()).
                event(Terminated.class, (event,state) -> stop(new Failure("Child calc stopped."), state))
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
}
