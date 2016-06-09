package sample.hello;

import akka.actor.*;
import com.typesafe.config.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static java.lang.Integer.parseInt;

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

        String systemName = "server";
        Config config = ConfigFactory.load("application");
        config = config.getConfig(systemName);
        config = config.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(parseInt(port)));
        //System.out.println(config);
        ActorSystem system = ActorSystem.create(systemName, config);
        ActorRef a = system.actorOf(Props.create(Server.class), "Server");

        ActorRef shard = system.actorOf(Props.create(Server.class), "shard");

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
