package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.tr.analytics.sage.akka.data.CalcResultCore;
import com.tr.analytics.sage.akka.data.CalcUpdateCore;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;

/*
Usage:
    <launcher> shard
    or
    <launcher> asm
 */
public class Launcher {
    public static final String SHARED_SECTION_NAME = "common";
    public static final String ASSEMBLER_SYSTEM_NAME = "sage-assembler";
    public static final String SHARD_SYSTEM_NAME = "sage-shard";
    public static final String TRADE_SOURCE_SYSTEM_NAME = "sage-trades";
    //public static String CLIENT_SYSTEM_NAME = "";

    public static final String ARG_SHARD = "shard";
    public static final String ARG_ASSEMBLER = "asm";
    public static final String ARG_TRADESOURCE = "trades";
    public static final String ARG_CLIENT = "client";

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
        else if (arg.equals(ARG_CLIENT))
        {
            int countSubscriptions = port;
            LaunchClient(countSubscriptions);
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
                        "    <launcher> " + ARG_TRADESOURCE + " <port> <trade_replay_file_path>\n" +
                        "    or\n" +
                        "    <launcher> " + ARG_CLIENT + " <count_subsc>\n"
        );
    }

    public static LinkedList<String> loadRics() {
        LinkedList<String> rics = new LinkedList<>();
        try {
            File file = new File("c:\\dev\\test\\rics\\log.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            int skip = 0;
            int count = 0;
            while ((line = bufferedReader.readLine()) != null) {

                count++;

                if (count > skip) {
                    if (line.startsWith("++RicStore(")) {
                        String ric = line.substring("++RicStore(".length(), line.indexOf(")"));
                        //stringBuffer.append("\"" + ric + "\", ");
                        rics.add(ric);
                    }

//                    if (count > skip+1000) {
//                        break;
//                    }
                }
            }
            fileReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return rics;
    }

    public static void LaunchAssembler(int port) throws Exception {
        LaunchAssembler(port, true);
    }

    public static void LaunchAssembler(int port, boolean waitForShutdown) throws Exception {
        Config appConfig = ConfigFactory.load("application");
        Config config = appConfig.getConfig(ASSEMBLER_SYSTEM_NAME).withFallback(appConfig.getConfig(SHARED_SECTION_NAME));
        config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));

        ActorSystem system = ActorSystem.create(ASSEMBLER_SYSTEM_NAME, config);
        CriticalActorWatcher.create(system);

        ActorRef assembler = system.actorOf(Props.create(Assembler.class), Assembler.NAME);
        CriticalActorWatcher.watch(assembler);

        //runClient(system);


        System.out.println(Assembler.NAME + " - started");

        if (waitForShutdown) {
            Future f = system.whenTerminated();
            Await.result(f, Duration.Inf());


            System.out.println(Assembler.NAME + " - stopped");
        }
    }

    public static void LaunchClient(int countSubscriptions) throws Exception {

        ScriptDriver driver = new ScriptDriver();



        class TestActor extends ScriptUntypedActor
        {
            TestActor(Object data)
            {
                super(data);
            }

            @Override
            public void onReceiveCore(Object message, Object data) {
                if (message instanceof CalcResultCore || message instanceof CalcUpdateCore) {
                    int count = ((AtomicInteger) data).incrementAndGet();
                    if (count % (10 * 1000) == 0) {
                        System.out.println(">>> Got " + count + " messages ...");
                    }
                }
            }
        }

        final LinkedList<ActorRef> actors = new LinkedList<>();

        final AtomicInteger counter = new AtomicInteger(0);

        int req = 0;
        for (int i = 0; i < countSubscriptions; i++)
        {
            ActorRef actor = driver.system().actorOf(Props.create(TestActor.class, counter));
            actors.push(actor);
            driver.asm().tell(new StartCalcMultiRic("VWAP", "CLIENT", req++, false, Arrays.asList("*")), actor);
        }

        System.out.println("Created " + countSubscriptions + " subscriptions.");

        for (ActorRef actor : actors)
        {
            CriticalActorWatcher.watch(actor);
        }

        System.out.println("Press a key to exit.");
        System.in.read();

        for (ActorRef actor : actors)
        {
            driver.system().stop(actor);
        }

        driver.shutdown();
    }

    private static void runClient(ActorSystem system) {
        // TODO: remove - test only
        ActorRef client = system.actorOf(Props.create(Client.class), "TestClient");
        CriticalActorWatcher.watch(client);

        int req = 0;
        Iterable<String> rics = Arrays.asList("*"); //55834583239"); //loadRics();
        //System.out.println(">>> " + loadRics().size());

        int batchSize = 300;
        LinkedList<String> ricsBatch = new LinkedList<>();
        int count = 0;

//        for (String ric : rics) {
//
//            ricsBatch.add(ric);
//
//            if (count++ == batchSize) {
//                client.tell(new StartCalcMultiRic("start", "TradeSource-test", req++, ricsBatch), system.deadLetters());
//                ricsBatch = new LinkedList<>();
//                count = 0;
//            }
//        }
        client.tell(new StartCalcMultiRic("start", "TradeSource", req++, ricsBatch), system.deadLetters());

        client.tell(new StartCalcMultiRic("VWAP", "VWAP", req++, rics), system.deadLetters());
//        client.tell(new StartCalcMultiRic("VWAP", "VWAP-test2", req++, rics2), system.deadLetters());
//        client.tell(new StartCalcMultiRic("VWAP", "VWAP-test3", req++, rics3), system.deadLetters());
//        client.tell(new StartCalcMultiRic("VWAP", "VWAP-test4", req++, rics4), system.deadLetters());
//        client.tell(new StartCalcMultiRic("VWAP", "VWAP-test5", req++, rics5), system.deadLetters());
//        client.tell(new StartCalcMultiRic("VWAP", "VWAP-test6", req++, rics6), system.deadLetters());
        /*
        55837234180,821710
55866133913,646140
55835357700,563750
55855393039,549640
55835362916,521880
55835328323,458200
         */
    }

    private static void LaunchShard(int port) throws Exception {
        Config appConfig = ConfigFactory.load("application");
        Config config = appConfig.getConfig(SHARD_SYSTEM_NAME).withFallback(appConfig.getConfig(SHARED_SECTION_NAME));
        config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));

        ActorSystem system = ActorSystem.create(SHARD_SYSTEM_NAME, config);
        CriticalActorWatcher.create(system);

        ExecutionContext dispatcherLongCalc = system.dispatchers().lookup(Shard.LONG_CALC_DISPATCHER_NAME);
        if (null == dispatcherLongCalc)
        {
            throw new Exception(String.format("Unable to lookup dispatcher: {}. Will use current context's one.", Shard.LONG_CALC_DISPATCHER_NAME));
        }

        ActorRef shard = system.actorOf(Props.create(Shard.class, dispatcherLongCalc), Shard.NAME);
        CriticalActorWatcher.watch(shard);

        System.out.println(Shard.NAME + " - started");

        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());

        System.out.println(Shard.NAME + " - stopped");
    }

    private static void LaunchTradeSource(int port, String replayPath) throws Exception {
        Config appConfig = ConfigFactory.load("application");
        Config config = appConfig.getConfig(TRADE_SOURCE_SYSTEM_NAME).withFallback(appConfig.getConfig(SHARED_SECTION_NAME));
        config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));


        ActorSystem system = ActorSystem.create(TRADE_SOURCE_SYSTEM_NAME, config);
        CriticalActorWatcher.create(system);

        ActorRef source = system.actorOf(Props.create(TradeSource.class, replayPath), TradeSource.NAME);
        CriticalActorWatcher.watch(source);

        System.out.println(TradeSource.NAME + " - started");

        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());

        System.out.println(TradeSource.NAME + " - stopped");
    }



}
