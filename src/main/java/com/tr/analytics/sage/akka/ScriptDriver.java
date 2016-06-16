package com.tr.analytics.sage.akka;


import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.routing.FromConfig;
import akka.util.Timeout;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
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
    private static final String SAGE_SCRIPT_CLIENT_SYSTEM_NAME = "sage-script-client";
    private final ActorRef assembler;
    private final ActorSystem system;

    int req = 0;

    public ScriptDriver() {
        //Config appConfig = ConfigFactory.parseFile(new File(configPath));
        //ConfigFactor.s
        Config appConfig = ConfigFactory.load(ScriptDriver.class.getClassLoader(), "application");
        Config reference = ConfigFactory.load(ScriptDriver.class.getClassLoader(), "reference");
        Config config = appConfig.getConfig(SAGE_SCRIPT_CLIENT_SYSTEM_NAME).withFallback(appConfig.getConfig(SHARED_SECTION_NAME)).withFallback(reference);
        //config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));

        //System.out.println("1");
        system = ActorSystem.create(SAGE_SCRIPT_CLIENT_SYSTEM_NAME, config, ScriptDriver.class.getClassLoader());
        //System.out.println("2");
        CriticalActorWatcher.create(system);

        assembler = system.actorOf(FromConfig.getInstance().props(), Assembler.NAME);
        CriticalActorWatcher.watch(assembler);


        //ActorRef assembler = system.actorOf(Props.create(Assembler.class), Assembler.NAME);
        //CriticalActorWatcher.watch(assembler);

        // TODO: remove - test only
        //client = system.actorOf(Props.create(Client.class), "com.tr.analytics.sage.akka.ScriptDriver");
        //CriticalActorWatcher.watch(client);

        System.out.println(Client.NAME + " - started");
    }

    public ActorSystem system()
    {
        return system;
    }


    public ActorRef asm()
    {
        return assembler;
    }

    public Object makeStart(String name, String instance, boolean isSnapshot, String[] rics)
    {
        return new StartCalcMultiRic(name, instance, req++, Arrays.asList(rics));
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
        return Duration.create(fromNano - nanoTime(), TimeUnit.NANOSECONDS);
    }

    public Object ask(ActorRef askTo, Object message, FiniteDuration timeout) throws Exception {
        Future<Object> f = Patterns.ask(askTo, message, new Timeout(timeout));
        return Await.result(f, timeout);
    }

    public Object ask(ActorSelection askTo, Object message, FiniteDuration timeout) throws Exception {
        Future<Object> f = Patterns.ask(askTo, message, new Timeout(timeout));
        return Await.result(f, timeout);
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
