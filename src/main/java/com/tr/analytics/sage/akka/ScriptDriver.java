package com.tr.analytics.sage.akka;


import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.routing.FromConfig;
import akka.util.Timeout;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.tr.analytics.sage.akka.data.TestVisitor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.tr.analytics.sage.akka.Launcher.SHARED_SECTION_NAME;

public class ScriptDriver {
    private static final String SAGE_CLIENT_SYSTEM_NAME = "sage-client";
    private final ActorRef assembler;
    private final ActorSystem system;
    private final ActorRef tradeSources;
    private final ActorRef tradeRouters;

    int req = 0;

    public ScriptDriver() {
        System.out.println(SAGE_CLIENT_SYSTEM_NAME + " - starting");

        //Config appConfig = ConfigFactory.parseFile(new File(configPath));
        //ConfigFactor.s
        Config appConfig = ConfigFactory.load(ScriptDriver.class.getClassLoader(), "application");
        Config reference = ConfigFactory.load(ScriptDriver.class.getClassLoader(), "reference");
        Config config = appConfig.getConfig(SAGE_CLIENT_SYSTEM_NAME).withFallback(appConfig.getConfig(SHARED_SECTION_NAME)).withFallback(reference);
        //config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));


        system = ActorSystem.create(SAGE_CLIENT_SYSTEM_NAME, config, ScriptDriver.class.getClassLoader());
        CriticalActorWatcher.create(system);

        assembler = system.actorOf(FromConfig.getInstance().props(), Assembler.NAME);
        CriticalActorWatcher.watch(assembler);

        tradeSources = system.actorOf(FromConfig.getInstance().props(), TradeSource.NAME);
        tradeRouters = system.actorOf(FromConfig.getInstance().props(), TradeRouter.NAME);

        //ActorRef assembler = system.actorOf(Props.create(Assembler.class), Assembler.NAME);
        //CriticalActorWatcher.watch(assembler);

        // TODO: remove - test only
        //client = system.actorOf(Props.create(Client.class), "com.tr.analytics.sage.akka.ScriptDriver");
        //CriticalActorWatcher.watch(client);

        System.out.println(SAGE_CLIENT_SYSTEM_NAME + " - started");
    }


    public ScriptDriver(ActorSystem system, ActorRef assembler) {
        this.system = system;
        this.assembler = assembler;

        tradeSources = system.actorFor(TradeSource.NAME);
        tradeRouters = system.actorFor(TradeRouter.NAME);
    }

    public ActorSystem system()
    {
        return system;
    }


    public ActorRef asm()
    {
        return assembler;
    }

    public ActorRef sources() {
        return tradeSources;
    }

    public ActorRef tradeRouters() {
        return tradeRouters;
    }

    public StartCalcMultiRic makeReq(String name, String instance, boolean isSnapshot, String[] rics)
    {
        return new StartCalcMultiRic(name, instance, req++, isSnapshot, Arrays.asList(rics));
    }

    public TestVisitor makeVerb(String verb, Object data)
    {
        return new TestVisitor(verb, data);
    }

    public TestVisitor makeVerb(String verb)
    {
        return new TestVisitor(verb, null);
    }



    public Duration duration(String duration)
    {
        return Duration.create(duration);
    }

    public long nanoTime()
    {
        return System.nanoTime();
    }

    public FiniteDuration elapsed(long fromNano)
    {
        return Duration.create(nanoTime() - fromNano, TimeUnit.NANOSECONDS);
    }

    public Object ask(ActorRef askTo, Object message, FiniteDuration timeout) throws Exception {
        Future<Object> f = Patterns.ask(askTo, message, new Timeout(timeout));
        return Await.result(f, timeout);
    }

    public Object ask(ActorRef askTo, Object message, String timeout) throws Exception {
        FiniteDuration duration = (FiniteDuration)Duration.create(timeout);
        Future<Object> f = Patterns.ask(askTo, message, new Timeout(duration));
        return Await.result(f, duration);
    }

    public Object ask(ActorSelection askTo, Object message, FiniteDuration timeout) throws Exception {
        Future<Object> f = Patterns.ask(askTo, message, new Timeout(timeout));
        return Await.result(f, timeout);
    }

    public Object ask(ActorSelection askTo, Object message, String timeout) throws Exception {
        FiniteDuration duration = (FiniteDuration)Duration.create(timeout);
        Future<Object> f = Patterns.ask(askTo, message, new Timeout(duration));
        return Await.result(f, duration);
    }


//    Duration timedAsk(ActorRef askTo, Object message, Duration timeout)
//    {
//        long start = this.nanoTime();
//
//
//    }
//
//    Duration timedAsk(ActorRef actorRef, Object message)
//    {
//
//    }

//    public void wait(int count, String duration)
//    {}
//
//    public void waitAll(String duration)
//    {}



    public void shutdown() throws Exception {
        System.out.println(Client.NAME + " - stopping");
        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());
        System.out.println(Client.NAME + " - stopped");
    }
}
