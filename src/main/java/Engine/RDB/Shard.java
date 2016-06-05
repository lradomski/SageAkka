package Engine.RDB;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import java.util.HashMap;


public class Shard extends UntypedActor {
    private final HashMap<String, RicStore> ricStores = new HashMap<String, RicStore>();
    private ActorRef tradeSource;

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
