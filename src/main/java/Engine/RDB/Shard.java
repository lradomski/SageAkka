package Engine.RDB;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.tr.analytics.sage.akka.RicStore;

import java.util.HashMap;


public class Shard extends UntypedActor {


    public Shard()
    {}

    @Override
    public void preStart() throws Exception {
        super.preStart();
    }

    @Override
    public void onReceive(Object o) throws Exception {

    }
}
