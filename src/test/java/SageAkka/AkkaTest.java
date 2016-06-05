package SageAkka;

import akka.actor.*;
import akka.japi.Function;
import akka.pattern.Backoff;
import akka.pattern.BackoffSupervisor;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import akka.testkit.TestActorRef;
import akka.testkit.TestActors;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import sample.hello.HelloWorld;
import scala.Option;
import scala.PartialFunction;
import scala.collection.mutable.Stack;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


public class AkkaTest extends TestCase {

    ActorSystem system;


    public AkkaTest(String name) {
        super(name);
    }

    static class A1 extends UntypedActor
    {
        static public enum Life { INIT, STARTED,STOPPED, RESTARTED, TERMINATED };
        private Life life = Life.INIT;
        Stack<Life> lifeHistory = new Stack<Life>();

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
            }
            else if (m.equals("throw"))
            {
                throw new Exception("test");
            }
            else if (m instanceof Terminated)
            {
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

        private static Function<Throwable, SupervisorStrategy.Directive> decider = new Function<Throwable, SupervisorStrategy.Directive>() {
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
        system.terminate().wait();
    }

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





}