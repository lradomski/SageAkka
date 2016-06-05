package sample.hello;

import akka.actor.*;
import akka.remote.RemoteScope;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.sleep;

public class MainClient {
    static public class Slave extends UntypedActor
    {
        @Override
        public void onReceive(Object o) throws Exception {
            System.out.println("Slave: " + o);
        }
    }

    static public class Client extends UntypedActor
    {
        final Router router = new Router(new BroadcastRoutingLogic());
        private ActorRef server = null;
        private ActorRef slave = null;

        @Override
        public void preStart() throws Exception {

            String address = "akka.tcp://server@192.168.1.9:3000";
            String path = address + "/user/Server";
            getContext().actorSelection(address + "/user/Server").tell(new Identify(path), getSelf());
//            ActorRef ref = getContext().actorOf(Props.create(MainServer.Server.class, "RemoteServer").withDeploy(
//                    new Deploy(new RemoteScope(AddressFromURIString.parse(address)))));
            //final String path = "akka.tcp://CalculatorSystem@127.0.0.1:2552/user/calculator";
//            final ActorRef ref = getContext().actorOf(
//                    Props.create(MainServer.Server.class, path)); //, "Server");
//            router.addRoutee(ref);

            slave = getContext().actorOf(Props.create(Slave.class));
            router.addRoutee(slave);
        }

        @Override
        public void onReceive(Object o) throws Exception {
            System.out.println(o);

            if (o instanceof ActorIdentity)
            {
                server = ((ActorIdentity)o).getRef();
                if (null != server) {
                    //router.addRoutee(server);
                    getContext().watch(server);
                    server.tell("extra", getSelf());
                    System.out.println("Got identity: " + server.toString());//getSender().tell("extra", getSelf());
                }
                else
                {
                    System.out.println("No identity");
                }
            }
            else if (o instanceof String)
            {
                router.route(">> " + (String)o, getSelf());
                if (null != server) server.tell(o, getSelf());
            }

        }
    }

    public static void main(String[] args) throws Exception {
//        if (args.length == 0)
//        {
//            System.out.println("Needs port argument (integer).");
//        }

        final String port = "4000"; //args[0];
        Config config = ConfigFactory.parseString(
                "akka {\n" +
                        "\n" +
                        "  stdout-loglevel = \"DEBUG\"\n" +
                        "  actor {\n" +
                        "    provider = \"akka.remote.RemoteActorRefProvider\"\n" +
                        "  }\n" +
                        "\n" +
                        "  remote {\n" +
                        "    log-sent-messages = on\n" +
                        "    log-received-messages = on\n" +
                        "    log-remote-lifecycle-events = on\n" +
                        "    enabled-transports = [\"akka.remote.netty.tcp\"]\n" +
                        "    netty.tcp {\n" +
                        "      hostname = \"192.168.1.9\"\n" +
                        "      port = " + port + "\n" +
                        "    }\n" +
                        "  }\n" +
                        "\n" +
                        "}\n");
        // ConfigFactory.load sandwiches customConfig between default reference
        // config and default overrides, and then resolves it.
        //ActorSystem system = ActorSystem("MySystem", ConfigFactory.load(customConf))
        ActorSystem system = ActorSystem.create("client", config);

        System.out.println("Client - started");

        ActorRef client = system.actorOf(Props.create(Client.class), "Client");


        for (int i = 0; i < 1000; i++) {
            sleep(3 * 1000);
            client.tell("bcast!", client);
        }
        //sleep(1*1000);
        //system.stop(client);

        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());


        System.out.println("Client - stopped");
    }
}
