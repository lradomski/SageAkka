package com.tr.analytics.sage.akka;

import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.util.Arrays;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class ScriptClient {
    private static final String SAGE_SCRIPT_CLIENT_SYSTEM_NAME = "sage-script-client";
    private final ActorRef client;
    private ActorSystem system;
    int req = 0;

    public ScriptClient(String configPath, String name) {
        Config config = ConfigFactory.load("application").getConfig(SAGE_SCRIPT_CLIENT_SYSTEM_NAME);
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

    public void req(String name, String instance, String[] rics)
    {
        client.tell(new StartCalcMultiRic(name, instance, req++, Arrays.asList(rics)), client);
    }

    public void req(String name, String[] rics)
    {
        client.tell(new StartCalcMultiRic(name, Integer.toString(req), req++, Arrays.asList(rics)), client);
    }


    public void reqAll(String name)
    {
        client.tell(new StartCalcMultiRic(name, Integer.toString(req), req++, Arrays.asList("*")), client);
    }

    public void shutdown() throws Exception {
        System.out.println(Client.NAME + " - stopping");
        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());
        System.out.println(Client.NAME + " - stopped");
    }
}
