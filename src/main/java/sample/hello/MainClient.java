package sample.hello;

import akka.actor.*;
import akka.pattern.Patterns;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.FromConfig;
import akka.routing.Router;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.dispatch.Futures.future;

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
                    Future<Object> f = future(() -> { FindServer(); return null; }, getContext().dispatcher());
                    getContext().system().scheduler().scheduleOnce(Duration.create(3, TimeUnit.SECONDS), new Thread(() -> FindServer()), getContext().dispatcher());
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

                System.out.println("SimpleClient - stopping ...");
                getContext().system().terminate();

            }

        }
    }

    public static void main(String[] args) throws Exception {

        final String port = "4000";
        Config config = ConfigFactory.load("application").getConfig("client");

        ActorSystem system = ActorSystem.create("client", config);

        System.out.println("SimpleClient - started");

        ActorRef client = system.actorOf(Props.create(Client.class), "SimpleClient");
        ActorRef shards = system.actorOf(FromConfig.getInstance().props(), "shards");

        shards.tell(new Identify(2), client);
        shards.tell("shard broadcast", null);


        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());


        System.out.println("SimpleClient - stopped");
    }
}
