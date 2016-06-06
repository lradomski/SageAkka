package sample.hello;

import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class MainServer {

    static public class Root extends UntypedActor
    {
        @Override
        public void preStart() throws Exception {
            ActorRef a = getContext().actorOf(Props.create(Server.class), "Server");
            a.tell("test from root", getSelf());
        }

        @Override
        public void onReceive(Object o) throws Exception {

        }
    }
    static public class Server extends UntypedActor
    {
        @Override
        public void preStart() throws Exception {
            System.out.println(getSelf().path());
        }

        @Override
        public void onReceive(Object o) throws Exception {
            System.out.println(o);
            getContext().watch(getSender());

            if (o instanceof Terminated)
            {
                System.out.println("Terminating ...");
                getContext().stop(getSelf());
                getContext().system().terminate();
            }

        }
    }

    public static void main(String[] args) {
        if (args.length == 0)
        {
            System.out.println("Needs port argument (integer).");
        }

        final String port = args[0];
        Config config = ConfigFactory.parseString(
                "akka {\n" +
                        "\n" +
                        "  stdout-loglevel = \"DEBUG\"\n" +
                        "  actor {\n" +
                        "    provider = \"akka.remote.RemoteActorRefProvider\"\n" +
                        "  }\n" +
                        "\n" +
                        "  remote {\n" +
                        "    enabled-transports = [\"akka.remote.netty.tcp\"]\n" +
                        "    log-sent-messages = on\n" +
                        "    log-received-messages = on\n" +
                        "    log-remote-lifecycle-events = on\n" +
                        "    netty.tcp {\n" +
                        "      hostname = \"127.0.0.1\"\n" +
                        "      port = " + port + "\n" +
                        "    }\n" +
                        "  }\n" +
                        "\n" +
                        "}\n");
        // ConfigFactory.load sandwiches customConfig between default reference
        // config and default overrides, and then resolves it.
        //ActorSystem system = ActorSystem("MySystem", ConfigFactory.load(customConf))
        ActorSystem system = ActorSystem.create("server", config);
        //ActorRef root = system.actorOf(Props.create(Root.class), "Root");
        ActorRef a = system.actorOf(Props.create(Server.class), "Server");
        //a.tell("test", a);

        System.out.println("Server - started at port: " + port);

        Future f = system.whenTerminated();
        try
        {
            Await.result(f, Duration.Inf());
        }
        catch (Exception e)
        {
            System.out.println("Server - ??? ");
        }

        System.out.println("Server - terminated at port: " + port);
    }
}
