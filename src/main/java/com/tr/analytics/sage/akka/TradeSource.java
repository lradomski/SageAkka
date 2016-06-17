package com.tr.analytics.sage.akka;


import akka.actor.*;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import com.tr.analytics.sage.akka.data.SageIdentify;
import com.tr.analytics.sage.akka.data.SageIdentity;
import com.tr.analytics.sage.akka.data.TestVisitor;
import com.tr.analytics.sage.api.Trade;
import com.tr.analytics.sage.apps.LoadTradeCsv;
import com.tr.analytics.sage.shard.TradeReal;
import com.tr.analytics.sage.shard.TradeReceiver;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static akka.dispatch.Futures.future;

public class TradeSource extends AbstractFSMWithStash<TradeSource.States, TradeSource.State> {

    public static final String NAME = "trade-source";
    public final String replayPath;

    public static enum States {Idle, Streaming };

    public static final class State
    {
        AtomicBoolean keepStreaming = new AtomicBoolean(false);
        Router router = new Router(new BroadcastRoutingLogic());
        boolean gotTrade = false;
    }

    private static final String START_VERB = "start";
    private static final String STOP_VERB = "stop";

    public TradeSource(String replayPath)
    {
        this.replayPath = replayPath;
    }


    public static Props props(final String replayPath) {
        return Props.create(TradeSource.class, (Creator<TradeSource>) () -> new TradeSource(replayPath));
    }

    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    {
        startWith(States.Idle, new State());

        when(States.Idle,
                matchEvent(TestVisitor.class, (event, state) -> ifStartCmdStreamGoTo(event, state, States.Streaming)).
                event(SageIdentify.class, this::handleIdentify).
                event(TradeReal.class, this::handleTrade)
        );

//        onTransition(
//                matchState(States.Idle, States.Streaming, (from,to) -> { log().info("Streaming started.");})
//        );

        when(States.Streaming,
                matchEvent(DoneStreaming.class, (event,state) -> goTo(States.Idle)).
                event(SageIdentify.class, this::handleIdentify).
                event(TestVisitor.class, (event, state) -> ifStopSignalStopStay(event, state)).
                event(TradeReal.class, this::handleTrade)
        );

        onTransition(
                matchState(States.Streaming, States.Idle, (from,to) -> { log().info("Streaming stopped.");})
        );


        whenUnhandled(
                matchAnyEvent((event, state) -> {
                    log().warning("TradeSource received unhandled event {} in state {}/{}",
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
//                matchState(States.Idle, States.Streaming, (from, to) -> setTimer("tradeCheck", "tradeCheck", Duration.create(1, TimeUnit.SECONDS))).
//                state(States.Streaming, States.Idle, (from, to) -> cancelTimer("tradeCheck"))
//        );

//        onTransition(
//                matchState(null, States.Idle, (from,to) -> {}).
//                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
//        );

        initialize();

    }

    private FSM.State<States,State> ifStopSignalStopStay(TestVisitor event, State state) {
        if (event.getVerb().equals(STOP_VERB))
        {
            log().info("Signalling to stop streaming trades ...");
            state.keepStreaming.set(false);
        }
        return stay();
    }

    private static class DoneStreaming
    {}

    private static class TradeForwarder implements TradeReceiver
    {
        final Consumer<Trade> consumer;

        public TradeForwarder(Consumer<Trade> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void addTrade(Trade trade) {
            consumer.accept(trade);
        }
    }

    private FSM.State<States, State> ifStartCmdStreamGoTo(TestVisitor event, State state, States newState) {

        if (!event.getVerb().equals(START_VERB))
        {
            return stay();
        }

        state.keepStreaming.set(true);
        int rateMs = 100;
        int intervalMs = 50;

//        ActorRef throttler = context().actorOf(Props.create(TimerBasedThrottler.class,
//                new Throttler.Rate(intervalMs*rateMs, Duration.create(intervalMs, TimeUnit.MILLISECONDS))
//        ));
//        throttler.tell(new Throttler.SetTarget(self()), null); // Set the target
        final int stopAt = event.getData() instanceof Integer ? (Integer)event.getData() : 1000;
        Future<DoneStreaming> streamTrades = future(() -> runStreaming(state.keepStreaming, replayPath, stopAt, self()), context().dispatcher());

        // send completion result to self
        Patterns.pipe(streamTrades, context().dispatcher()).to(self());
        return goTo(newState);

    }

    private DoneStreaming runStreaming(AtomicBoolean keepStreaming, String replayPath, int stopAt, ActorRef forwardTo) throws IOException {
        //"C:\\dev\\SageAkka\\Trades_20160314.csv.gz"
        log().info("Streaming trades...");
        LoadTradeCsv.loadCsvCore(replayPath, trade -> { forwardTo.tell(trade, forwardTo); return keepStreaming.get();}, stopAt); // 34*1000*1000); //1000L*1000L*1000L); //1000*1000);
        log().info("Streaming trades stopped.");
        return new DoneStreaming();
    }


    private FSM.State<States, State> handleIdentify(SageIdentify event, State state) {
        context().watch(sender());
        state.router = state.router.addRoutee(sender());
        return stay().replying(SageIdentity.from(event, self()));
    }

    private FSM.State<States,State> handleTerminated(Terminated event, State state) {
        state.router = state.router.removeRoutee(event.actor());
        return stay();
    }

    private FSM.State<States,State> handleTrade(TradeReal event, State state) {
        // TODO: per-subscriber fan-out
        state.gotTrade = true;
        state.router.route(event, self());
        return stay();
    }

}
