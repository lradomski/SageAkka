package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.ControlMessage;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.tr.analytics.sage.shard.engine.TradeReal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TradeRouter extends UntypedActor{
    public final static String NAME = "ric";

    private final HashMap<String,ActorRef> rics = new HashMap<>();

    public static class RicStoreRefs implements ControlMessage, Serializable
    {
        public static class RicStoreRef
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
        private final RicStoreRef[] ricRefs;

        public RicStoreRefs(RicStoreRef[] ricRefs) {
            this.ricRefs = ricRefs;
        }

        // TODO: return immutable
        public RicStoreRef[] getRicRefs() {
            return ricRefs;
        }
    }

    @Override
    public void onReceive(Object m) throws Exception
    {
        if (m instanceof TradeReal)
        {
            String ric = Long.toString(((TradeReal) m).getQuoteId());//getRic();
            ActorRef ricStore = rics.get(ric);
            if (null == ricStore)
            {
                ricStore = context().actorOf(Props.create(RicStore.class, ric), ric);
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

            RicStoreRefs.RicStoreRef[] a = new RicStoreRefs.RicStoreRef[ricRefs.size()];
            sender().tell(new RicStoreRefs(ricRefs.toArray(a)), self());
        }
    }
}
