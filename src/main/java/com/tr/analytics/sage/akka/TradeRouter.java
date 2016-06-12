package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.ControlMessage;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.tr.analytics.sage.shard.engine.TradeReal;
import common.ActorUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TradeRouter extends UntypedActor{
    public final static String NAME = "ric";

    private final HashMap<String,ActorRef> rics = new HashMap<>();

    public static class RicStoreRefs implements ControlMessage, Serializable
    {
        public static class RicStoreRef implements Serializable
        {
            private final String ric;
            private final ActorRef ricStore;

            public RicStoreRef(String ric, ActorRef ricStore) {
                this.ric = ric;
                this.ricStore = ricStore;
            }

            public String getRic() {
                return ric;
            }

            public ActorRef getRicStore() {
                return ricStore;
            }
        }

        // TODO: required for serlization - improve
        private final LinkedList<RicStoreRefs.RicStoreRef> ricRefs;

        public RicStoreRefs(LinkedList<RicStoreRefs.RicStoreRef> ricRefs) {
            this.ricRefs = ricRefs;
        }

        // TODO: return immutable
        public Iterable<RicStoreRef> getRicRefs() {
            return ricRefs;
        }
    }

    int idNext = 0;

    @Override
    public void onReceive(Object m) throws Exception
    {
        if (m instanceof TradeReal)
        {
            // TODO: getRic
            String ric = Long.toString(((TradeReal) m).getQuoteId());//getRic();
            ActorRef ricStore = rics.get(ric);
            if (null == ricStore)
            {
                ricStore = context().actorOf(Props.create(RicStore.class, ric), Integer.toString(idNext++) + "-" + ActorUtils.makeActorName(ric));
                rics.put(ric, ricStore);
            }

            ricStore.tell(m, getSender());
        }
        else if (m instanceof StartCalcMultiRic)
        {
            LinkedList<RicStoreRefs.RicStoreRef> ricRefs = new LinkedList<>();

            for (String ric : ((StartCalcMultiRic) m).getRics())
            {
                ActorRef ricStore = rics.get(ric);
                if (null != ricStore)
                {
                    ricRefs.add(new RicStoreRefs.RicStoreRef(ric, ricStore));
                }
            }

            sender().tell(new RicStoreRefs(ricRefs), self());
        }
    }

    public boolean testHasRic(String ric)
    {
        return rics.get(ric) != null;
    }
    public boolean testHasRics()
    {
        return !rics.isEmpty();
    }
    public ActorRef testGetRicStore(String ric) { return rics.get(ric); }

}
