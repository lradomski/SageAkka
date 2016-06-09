package sample.hello;

import akka.actor.*;
import akka.remote.RemoteScope;
import akka.remote.testconductor.Terminate;
import akka.routing.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        Router router = new Router(new BroadcastRoutingLogic());
        Cancellable timer = null;

        @Override
        public void preStart() throws Exception {

            FindServer();
//            ActorRef ref = getContext().actorOf(Props.create(MainServer.Server.class, "RemoteServer").withDeploy(
//                    new Deploy(new RemoteScope(AddressFromURIString.parse(address)))));
            //final String path = "akka.tcp://CalculatorSystem@127.0.0.1:2552/user/calculator";
//            final ActorRef ref = getContext().actorOf(
//                    Props.create(MainServer.Server.class, path)); //, "Server");
//            router.addRoutee(ref);

            timer = getContext().system().scheduler().schedule(Duration.Zero(),
                    Duration.create(3000, TimeUnit.MILLISECONDS), getSelf(), "bcast!",
                    getContext().system().dispatcher(), null);

        }

        private void FindServer() {
            String address = "akka.tcp://server@127.0.0.1:3000";
            String path = address + "/user/Server";
            getContext().actorSelection(path).tell(new Identify(1), getSelf());
        }


        @Override
        public void onReceive(Object o) throws Exception {
            System.out.println(o);

            if (o instanceof ActorIdentity)
            {
                ActorRef server = ((ActorIdentity)o).getRef();
                if (null != server) {
                    router = router.addRoutee( new ActorRefRoutee(server) );
                    getContext().watch(server);
                    System.out.println("Got identity: " + server.toString());//getSender().tell("extra", getSelf());
                }
                else
                {
                    System.out.println("No identity");
                    FindServer();
                }
            }
            else if (o instanceof String)
            {
                router.route(">> " + (String)o, getSelf());
            }
            else if (o instanceof Terminated)
            {
                timer.cancel();
                getContext().system().stop(getSelf());

                System.out.println("Client - stopping ...");
                getContext().system().terminate();

            }

        }
    }

    public static void main(String[] args) throws Exception {

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
                        "      hostname = \"127.0.0.1\"\n" +
                        "      port = " + port + "\n" +
                        "    }\n" +
                        "  }\n" +
                        "\n" +

                        "actor.deployment {\n" +
                        "  /shards {\n" +
                        "    router = broadcast-group\n" +
                        "    routees.paths = [\n" +
                        "      \"akka.tcp://server@127.0.0.1:3000/user/shard\", \n" +
                        "      \"akka.tcp://server@127.0.0.1:3001/user/shard\",\n" +
                        "      \"akka.tcp://server@127.0.0.1:3002/user/shard\"]\n" +
                        "  }\n" +
                        "}" +
                        "}\n");

        ActorSystem system = ActorSystem.create("client", config);

        System.out.println("Client - started");

        ActorRef client = system.actorOf(Props.create(Client.class), "Client");
        ActorRef shards = system.actorOf(FromConfig.getInstance().props(), "shards");

        shards.tell(new Identify(2), client);
        shards.tell("shard broadcast", null);


//        for (int i = 0; i < 1000; i++) {
//            sleep(3 * 1000);
//            client.tell("bcast!", client);
//        }
        //sleep(1*1000);
        //system.stop(client);

        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());


        System.out.println("Client - stopped");
    }
}
