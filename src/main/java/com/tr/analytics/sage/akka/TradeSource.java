package com.tr.analytics.sage.akka;


import akka.japi.Creator;
import com.tr.analytics.sage.akka.data.SageIdentify;
import com.tr.analytics.sage.akka.data.SageIdentity;
import com.tr.analytics.sage.api.Trade;
import com.tr.analytics.sage.apps.LoadTradeCsv;
import com.tr.analytics.sage.shard.engine.TradeReal;
import com.tr.analytics.sage.shard.engine.TradeReceiver;

import java.io.IOException;
import java.util.function.Consumer;

import akka.actor.*;
import akka.pattern.Patterns;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static akka.dispatch.Futures.future;

public class TradeSource extends AbstractFSMWithStash<TradeSource.States, TradeSource.State> {

    public static final String NAME = "trade-source";
    public final String replayPath;

    public static enum States {Idle, Streaming };

    public static final class State
    {
        Router router = new Router(new BroadcastRoutingLogic());
        boolean gotTrade = false;
    }

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
                matchEvent(SageIdentify.class, this::handleIdentify).
                event(Terminated.class, this::handleTerminated).
                eventEquals("start", (event, state) -> goToStreaming())
        );

        when(States.Streaming,
                matchEvent(DoneStreaming.class, (event,state) -> goTo(States.Idle)).
                event(SageIdentify.class, this::handleIdentify).
                eventEquals("start", (event, state) -> stay()).
                event(TradeReal.class, this::handleTrade)
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

    private FSM.State<States, State> goToStreaming() {
        log().debug("Streaming trades");
        Future<DoneStreaming> streamTrades = future(() -> runStreaming(replayPath, self()), context().dispatcher());

        // send completion result to self
        Patterns.pipe(streamTrades, context().dispatcher()).to(self());
        return goTo(States.Streaming);

    }

    private static DoneStreaming runStreaming(String replayPath, ActorRef forwardTo) throws IOException {
        TradeForwarder forwarder = new TradeForwarder(trade -> forwardTo.tell(trade, forwardTo));
        //"C:\\dev\\SageAkka\\Trades_20160314.csv.gz"
        LoadTradeCsv.loadCsv(replayPath, forwarder, 10);
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
