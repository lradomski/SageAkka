package SageAkka;

import Common.Function_WithExceptions;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import akka.testkit.TestActorRef;
import akka.testkit.TestActors;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import scala.Option;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static Common.FuturesUtils.futureWithTimeout;
import static Common.FuturesUtils.toMapper;
import static SageAkka.AkkaTest.States.Compute1;
import static SageAkka.AkkaTest.States.Init;
import static akka.dispatch.Futures.future;
import static akka.dispatch.Futures.sequence;


public class AkkaTest extends TestCase {

    ActorSystem system;


    public AkkaTest(String name) {
        super(name);
    }

    static class A1 extends UntypedActor
    {
        public enum Life { INIT, STARTED,STOPPED, RESTARTED, TERMINATED }
        private Life life = Life.INIT;
        Stack<Life> lifeHistory = new Stack<>();

        private String lastMessage = null;

        @Override
        public void preStart() throws Exception {
            life = Life.STARTED;
            lifeHistory.push(life);
        }

        @Override
        public void preRestart(Throwable reason, Option<Object> message) throws Exception {
            life = Life.RESTARTED;
            lifeHistory.push(life);
        }

        @Override
        public void postStop() throws Exception {
            life = Life.STOPPED;
            lifeHistory.push(life);
        }

        @Override
        public void onReceive(Object m) throws Exception {
            if (m instanceof String) lastMessage = (String)m;

            if (m.equals("echo"))
            {
                int i = 0;
            }
            else if (m.equals("throw"))
            {
                throw new Exception("test");
            }
            else if (m instanceof Terminated)
            {
                int i = 0;
            }
            else if (m instanceof Status.Status)
            {
                System.out.println("Status: " + m.toString());
                int i = 0;
            }
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public Life getLife() {
            return life;
        }

    }

    static class A2 extends A1
    {

        private static akka.japi.Function<Throwable, SupervisorStrategy.Directive> decider = new akka.japi.Function<Throwable, SupervisorStrategy.Directive>() {
            public SupervisorStrategy.Directive apply(Throwable throwable) {
                return SupervisorStrategy.stop();
            }
        };

        private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), decider);

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return strategy;
        }


    }

    static class StashA1 extends UntypedActorWithStash
    {
        boolean stashing = false;
        @Override
        public void onReceive(Object o) throws Exception {
            if (o.equals("flip"))
            {
                if (stashing)
                {
                    unstashAll();
                    stashing = false;
                }
                else {
                    stashing = true;
                }
            }
            else if (o.toString().startsWith("special"))
            {
                System.out.println(o);
            }
            else
            {
                if (stashing)
                {
                    stash();
                }
                else
                {
                    System.out.println(o);
                }

            }
        }
    }

    public static Test suite()
    {
        return new TestSuite( AkkaTest.class );
    }

    @Override
    protected void setUp() throws Exception {
        system = ActorSystem.create("Test");
    }

    @Override
    protected void tearDown() throws Exception {
        system.terminate();
        Future f = system.whenTerminated();
        Await.result(f, Duration.Inf());    }

    public void testActorSelection()
    {
        String path = null;
        TestActorRef<A1> root = TestActorRef.create(system, Props.create(A1.class), "root");
        path = root.path().toStringWithoutAddress();

        TestActorRef<A1> c1 = root.create(system, Props.create(A1.class), root, "c1");
        path = c1.path().toStringWithoutAddress();

        TestActorRef<A1> c2 = root.create(system, Props.create(A1.class), root, "c2");
        path = c2.path().toStringWithoutAddress();

        Router r = new Router(new BroadcastRoutingLogic());
        r.addRoutee(system.actorSelection("/user/root/*")).route("echo-bcast", root);
        assertTrue("sent to all", c1.underlyingActor().getLastMessage() == "echo-bcast");
        assertTrue("sent to all", c2.underlyingActor().getLastMessage() == "echo-bcast");
    }

    public void testCrashActor()
    {

        TestActorRef<A2> root = TestActorRef.create(system, Props.create(A2.class), "root");
        TestActorRef<A2> c = TestActorRef.create(system, Props.create(A2.class), root, "child");
        root.watch(c);
        c.tell("throw", null);

    }

    public void  testStash()
    {
        TestActorRef<StashA1> root = TestActorRef.create(system, Props.create(StashA1.class), "stashing");
        System.out.println(">1");
        root.tell("1", null);

        System.out.println(">2");
        root.tell("2", null);

        System.out.println(">flip");
        root.tell("flip", null);

        System.out.println(">3");
        root.tell("3", null);

        System.out.println(">special-2");
        root.tell("special-2", null);

        System.out.println(">4");
        root.tell("4", null);

        System.out.println(">flip");
        root.tell("flip", null);

        System.out.println(">5");
        root.tell("5", null);

        System.out.println(">special-3");
        root.tell("special-3", null);

        System.out.println(">6");
        root.tell("6", null);

    }


    public void testPatterns() throws Exception
    {

        ActorRef a1 = system.actorOf(Props.create(TestActors.EchoActor.class), "a1");
        ActorRef a2 = system.actorOf(Props.create(TestActors.EchoActor.class), "a2");
        ActorRef a3 = system.actorOf(Props.create(TestActors.EchoActor.class), "a3");

        Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        Future<Object> f1 = Patterns.ask(a1, "1", timeout); //new Timeout(Duration.create(1, TimeUnit.NANOSECONDS)));
        Future<Object> f2 = Patterns.ask(a2, "2", timeout);
        Future<Object> f3 = Patterns.ask(a3, "3", timeout);


        Future<Iterable<Object>> fagg = sequence(Arrays.<Future<Object>>asList(f1, f2, f3), system.dispatcher());
        fagg = futureWithTimeout(fagg, Duration.create(1, TimeUnit.SECONDS), system.dispatcher(), system.scheduler());

        scala.concurrent.duration.FiniteDuration duration = Duration.create(1, TimeUnit.SECONDS);


        Future<String> result = fagg.map(toMapper(a -> a.toString()), system.dispatcher());
        Function_WithExceptions<Throwable, String, Exception> mapException = e -> { throw new Exception("Custom exception", e); };
        result = futureWithTimeout(result, duration, mapException, system);

        ActorRef test = system.actorOf(Props.create(A1.class), "test");
        Patterns.pipe(result, system.dispatcher()).to(test);
        Thread.sleep(3*1000);
        Future<Boolean> stop = akka.pattern.Patterns.gracefulStop(test,Duration.create(60, TimeUnit.SECONDS));
        Await.result(stop, Duration.Inf());

//        Future<Status.Status> faggs = fagg.map(new Mapper<Iterable<Object>, Status.Status>() {
//            public Status.Status apply(Iterable<Object> s) {
//                return new Status.Success(s);
//            }
//        }, system.dispatcher()).recover(new Recover<Status.Status>() {
//            public Status.Status recover(Throwable e) throws Throwable {
//                return new Status.Failure(e);
//            }
//        }, system.dispatcher());

//        Iterable<Object> r = Await.result(fagg, Duration.create(1, TimeUnit.SECONDS));
//        for(Object o : r)
//        {
//            System.out.print(o);
//        }
//        System.out.println();

//        Callable<String> callable = new Callable<String>() {
//            public String call() throws InterruptedException {
//                Thread.sleep(1000);
//                return "foo";
//            }
//        };
        Future<String> future = future(() -> {Thread.sleep(1000); return "foo";}, system.dispatcher());

        //scala.concurrent.duration.FiniteDuration duration = Duration.create(200, "millis");



//        result.onComplete(new OnComplete<String>() {
//            public void onComplete(Throwable failure, String result) {
//                if (failure != null) {
//                    //We got a failure, handle it here
//                } else {
//                    // We got a result, do something with it
//                }
//            }
//        }, system.dispatcher());

        //result.isCompleted();
    }


    public void testDispatcher() throws Exception
    {
        Config config = ConfigFactory.parseString(
                "akka {\n" +
                        "\n" +
                        "  stdout-loglevel = \"DEBUG\"\n" +
                        "\n" +
                        "}\n" +

                        "my-dispatcher {\n" +
                        "  # Dispatcher is the name of the event-based dispatcher\n" +
                        "  type = Dispatcher\n" +
                        "  # What kind of ExecutionService to use\n" +
                        "  executor = \"fork-join-executor\"\n" +
                        "  # Configuration for the fork join pool\n" +
                        "  fork-join-executor {\n" +
                        "    # Min number of threads to cap factor-based parallelism number to\n" +
                        "    parallelism-min = 1\n" +
                        "    # Parallelism (threads) ... ceil(available processors * factor)\n" +
                        "    parallelism-factor = 2.0\n" +
                        "    # Max number of threads to cap factor-based parallelism number to\n" +
                        "    parallelism-max = 1\n" +
                        "  }\n" +
                        "  # Throughput defines the maximum number of messages to be\n" +
                        "  # processed per actor before the thread jumps to the next actor.\n" +
                        "  # Set to 1 for as fair as possible.\n" +
                        "  throughput = 1\n" +
                        "}\n" +

                        "akka.actor.deployment {\n" +
                        "  \"/*\" {\n" +
                        "    dispatcher = blocking-io-dispatcher\n" +
                        "  }\n" +
                        "}\n" +

                        "my-pinned-dispatcher {\n" +
                        "  executor = \"thread-pool-executor\"\n" +
                        "  type = PinnedDispatcher\n" +
                        "}\n" +

                        "blocking-io-dispatcher {\n" +
                        "  type = Dispatcher\n" +
                        "  executor = \"thread-pool-executor\"\n" +
                        "  thread-pool-executor {\n" +
                        "    fixed-pool-size = 1\n" +
                        "  }\n" +
                        "  throughput = 1\n" +
                        "}\n"
        );

        if (null != system) system.terminate();
        system = ActorSystem.create("test", config);

        ActorRef a1 = system.actorOf(Props.create(StashA1.class), "a1");
        ActorRef a2 = system.actorOf(Props.create(StashA1.class), "a2");

        for (int i = 0; i < 10; i++)
        {
            a1.tell("> " + String.valueOf(i), null);
            a2.tell("= " + String.valueOf(i), null);
        }

        Thread.sleep(3*1000);



    }

    static enum States { Init, Compute1, Compute2, Final };

    public static class StateData
    {
        public int data1 = -1;
        public int data2 = -1;
        public int data3 = -1;

        public StateData compute(int i)
        {
            data1 = data2 = data3 = i;
            return this;
        }

        @Override
        public String toString() {
            return "{" + data1 + "," + data2 + "," + data3 + "}";
        }
    }

    public static class ActorFSM extends AbstractLoggingFSM<States, StateData>
    {
        public ActorFSM()
        {}

        {
            //new StateTimeout();
            startWith(Init, new StateData(), Duration.create(1, TimeUnit.SECONDS));

            when(Init,
                    matchEventEquals(Compute1, (event,state) -> goTo(Compute1).using(state.compute(10))).
                            eventEquals(StateTimeout(), (event,state) -> stop(new Failure("Timeout in Init1"), state)).
                            anyEvent((e,s) -> stay())
            );

            when(Compute1,
                    matchAnyEvent((event, state) -> stay())
            );

            // logging
            onTransition(
                    matchState(null, null, (from,to) -> System.out.println("from: " + from.toString() + ", to: " + to.toString() + ", data: " + stateData()))
                    //matchState(null, null, (from,to) -> System.out.println("from: " + from.toString() + ", to: " + to.toString() + ", data: " + stateData()))
                    //.state(null, Compute1, (from,to) -> System.out.println("> Compute1/" + from.toString() + to.toString()))
            );

            onTransition(
                matchState(null, Init, (from,to) -> System.out.println("> Init")).
                        state(null, Compute1, (from,to) -> System.out.println("> Compute1"))
            );

            initialize();

            ;
//
//                        matchState(null, Init, () -> ". > Init");
////                        matchState(Active, Idle, () -> setTimer("timeout",
////                                Tick, Duration.create(1, SECONDS), true)).
//                                //state(Active, null, () -> cancelTimer("timeout")).
//                                state(null, Init, (f, t) -> log().info("entering Idle from " + f)
//                                );


        }
    }

    public void testLambdaFSM() throws Exception
    {
        ActorRef fsm = system.actorOf(Props.create(ActorFSM.class), "fsm");
        fsm.tell(Init, null);
        Thread.sleep(3*1000);
        fsm.tell(Compute1, null);

        Thread.sleep(3*1000);
    }


}