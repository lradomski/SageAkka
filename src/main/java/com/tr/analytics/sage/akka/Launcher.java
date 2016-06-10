package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Identify;
import akka.actor.Props;
import akka.routing.FromConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/*
Usage:
    <launcher> shard
    or
    <launcher> asm
 */
public class Launcher {
    public static int main(String[] args) throws Exception {
        if (0 == args.length) {
            PrintUsage();
            return 1;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("shard"))
        {
            LaunchShard();
        }
        else if (arg.equals("asm"))
        {
            LaunchAssembler();
        }
        else
        {
            PrintUsage();
            return 1;
        }

        return 0;


    }

    private static void LaunchAssembler() throws Exception {
        final String systemName = "sage-assembler";

        Config config = ConfigFactory.load("application").getConfig(systemName);
        ActorSystem system = ActorSystem.create(systemName, config);

        ActorRef client = system.actorOf(Props.create(Client.class), "Client");
        ActorRef shards = system.actorOf(FromConfig.getInstance().props(), "shards");

        System.out.println("Assembler - started");

        shards.tell(new Identify(2), client);
        shards.tell("shard broadcast", null);


        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());


        System.out.println("Assembler - stopped");

    }

    private static void LaunchShard() throws Exception {
        final String systemName = "sage-shard";

        Config config = ConfigFactory.load("application").getConfig(systemName);
        ActorSystem system = ActorSystem.create(systemName, config);

        ActorRef client = system.actorOf(Props.create(Client.class), "Client");
        ActorRef shards = system.actorOf(FromConfig.getInstance().props(), "shards");

        System.out.println("Shard - started");

        shards.tell(new Identify(2), client);
        shards.tell("shard broadcast", null);


        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());


        System.out.println("Shard - stopped");

    }

    private static void PrintUsage() {
        System.out.println(
                "Usage:\n" +
                "    <launcher> shard\n" +
                "    or\n" +
                "    <launcher> asm");
    }

}
