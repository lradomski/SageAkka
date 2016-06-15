package scripting;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.tr.analytics.sage.akka.Assembler;
import com.tr.analytics.sage.akka.Client;
import com.tr.analytics.sage.akka.CriticalActorWatcher;
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

public class ScriptClient {
    private static final String SAGE_SCRIPT_CLIENT_SYSTEM_NAME = "sage-script-client";
    private final ActorRef client;
    private final ActorSystem system;
    int req = 0;

    public ScriptClient(String configPath, String name) {
        Config appConfig = ConfigFactory.load("application");
        Config config = appConfig.getConfig(SAGE_SCRIPT_CLIENT_SYSTEM_NAME).withFallback(appConfig.getConfig(SHARED_SECTION_NAME));
        //config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port));

        system = ActorSystem.create(SAGE_SCRIPT_CLIENT_SYSTEM_NAME, config);
        CriticalActorWatcher.Create(system);

        ActorRef assembler = system.actorOf(Props.create(Assembler.class), Assembler.NAME);
        CriticalActorWatcher.Watch(assembler);

        // TODO: remove - test only
        client = system.actorOf(Props.create(Client.class), "ScriptClient");
        CriticalActorWatcher.Watch(client);

        System.out.println(Client.NAME + " - started");
    }

    public ActorSystem system()
    {
        return system;
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
        return Duration.create(fromNano, TimeUnit.NANOSECONDS);
    }

    Object ask(ActorRef askTo, Object message, FiniteDuration timeout) throws Exception {
        Future<Object> f = Patterns.ask(askTo, message, new Timeout(timeout));
        return Await.result(f, timeout);
    }

    Object ask(ActorSelection askTo, Object message, FiniteDuration timeout) throws Exception {
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

    public void wait(int count, String duration)
    {}

    public void waitAll(String duration)
    {}



    public void shutdown() throws Exception {
        System.out.println(Client.NAME + " - stopping");
        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());
        System.out.println(Client.NAME + " - stopped");
    }
}
