package com.tr.analytics.sage.akka;

import com.tr.analytics.sage.akka.data.SageIdentity;

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Identify;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import scala.concurrent.duration.Duration;

public class CriticalActorWatcher extends AbstractFSM<CriticalActorWatcher.States, CriticalActorWatcher.State>
{
    public static enum States { Main; }
    public class State {};

    static ActorRef instance = null;

    private CriticalActorWatcher()
    {}

    public static void create(ActorSystem system)
    {
        if (null == instance) {
            instance = system.actorOf(Props.create(CriticalActorWatcher.class), "watcher");
        }
    }

    public static void watch(ActorRef toWatch)
    {
        instance.tell(new SageIdentity(0, toWatch), instance);
    }

    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void postStop() {
        super.postStop();
        context().system().shutdown();
    }

    {
        startWith(States.Main, new State());

        when(States.Main,
                matchEvent(Terminated.class, (event, state) -> stop()).
                event(SageIdentity.class, (event, state) -> { context().watch(event.getRef()); return stay();})
        );
    }
}

