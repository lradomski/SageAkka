package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Identify;
import akka.actor.Props;
import akka.routing.FromConfig;

import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.util.Arrays;
import java.util.List;

import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static java.lang.Integer.parseInt;

/*
Usage:
    <launcher> shard
    or
    <launcher> asm
 */
public class Launcher {
    public static String ASSEMBLER_SYSTEM_NAME = "sage-assembler";
    public static String SHARD_SYSTEM_NAME = "sage-shard";
    public static String TRADE_SOURCE_SYSTEM_NAME = "sage-trades";
    //public static String CLIENT_SYSTEM_NAME = "";

    public static String ARG_SHARD = "shard";
    public static String ARG_ASSEMBLER = "asm";
    public static String ARG_TRADESOURCE = "trades";

    public static void main(String[] args) throws Exception {
        mainCore(args);
    }

    public static int mainCore(String[] args) throws Exception {
        if (2 > args.length) {
            PrintUsage();
            return 1;
        }

        int port = parseInt(args[1]);

        String arg = args[0].toLowerCase();
        if (arg.equals(ARG_SHARD))
        {
            LaunchShard(port);
        }
        else if (arg.equals(ARG_ASSEMBLER))
        {
            LaunchAssembler(port);
        }
        else if (arg.equals(ARG_TRADESOURCE))
        {
            if (3 > args.length)
            {
                PrintUsage();
                return 1;
            }

            LaunchTradeSource(port, args[2]);
        }
        else
        {
            PrintUsage();
            return 1;
        }

        return 0;


    }

    private static void PrintUsage() {
        System.out.println(
                "Usage:\n" +
                        "    <launcher> " + ARG_ASSEMBLER + " <port>\n" +
                        "    or\n" +
                        "    <launcher> " + ARG_SHARD + " <port>\n" +
                        "    or\n" +
                        "    <launcher> " + ARG_TRADESOURCE + " <port> <trade_replay_file_path>\n"
        );
    }

    private static void LaunchAssembler(int port) throws Exception {
        Config config = ConfigFactory.load("application").getConfig(ASSEMBLER_SYSTEM_NAME);
        config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));

        ActorSystem system = ActorSystem.create(ASSEMBLER_SYSTEM_NAME, config);
        CriticalActorWatcher.Create(system);

        ActorRef assembler = system.actorOf(Props.create(Assembler.class), Assembler.NAME);
        CriticalActorWatcher.Watch(assembler);

        // TODO: remove - test only
        ActorRef client = system.actorOf(Props.create(Client.class), "TestClient");
        CriticalActorWatcher.Watch(client);

        int req = 0;
        //List<String> rics = Arrays.<String>asList("AAPL.O");
        List<String> rics = Arrays.<String>asList("55834583239");
        //55834583239
        client.tell(new StartCalcMultiRic("start", "TradeSource-test", req++, rics), system.deadLetters());
        Thread.sleep(3*1000);

        client.tell(new StartCalcMultiRic("VWAP", "VWAP-test", req++, rics), system.deadLetters());


        System.out.println(Assembler.NAME + " - started");

        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());


        System.out.println(Assembler.NAME + " - stopped");
    }

    private static void LaunchShard(int port) throws Exception {
        Config config = ConfigFactory.load("application").getConfig(SHARD_SYSTEM_NAME);
        config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));

        ActorSystem system = ActorSystem.create(SHARD_SYSTEM_NAME, config);
        CriticalActorWatcher.Create(system);

        ExecutionContext dispatcherLongCalc = system.dispatchers().lookup(Shard.LONG_CALC_DISPATCHER_NAME);
        if (null == dispatcherLongCalc)
        {
            throw new Exception(String.format("Unable to lookup dispatcher: {}. Will use current context's one.", Shard.LONG_CALC_DISPATCHER_NAME));
        }

        ActorRef shard = system.actorOf(Props.create(Shard.class, dispatcherLongCalc), Shard.NAME);
        CriticalActorWatcher.Watch(shard);

        System.out.println(Shard.NAME + " - started");

        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());

        System.out.println(Shard.NAME + " - stopped");
    }

    private static void LaunchTradeSource(int port, String replayPath) throws Exception {
        Config config = ConfigFactory.load("application").getConfig(TRADE_SOURCE_SYSTEM_NAME);
        config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));

        ActorSystem system = ActorSystem.create(TRADE_SOURCE_SYSTEM_NAME, config);
        CriticalActorWatcher.Create(system);

        ActorRef source = system.actorOf(Props.create(TradeSource.class, replayPath), TradeSource.NAME);
        CriticalActorWatcher.Watch(source);

        System.out.println(TradeSource.NAME + " - started");

        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());

        System.out.println(TradeSource.NAME + " - stopped");
    }



}
