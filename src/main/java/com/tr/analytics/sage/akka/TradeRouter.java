package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.dispatch.ControlMessage;
import com.tr.analytics.sage.akka.data.TestVisitor;
import scala.concurrent.duration.Duration;

import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.tr.analytics.sage.shard.TradeReal;
import com.tr.analytics.sage.akka.common.ActorUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TradeRouter extends UntypedActor{
    public final static String NAME = "ric";
    public static final String TESTVERB_CLEAN_STORE = "clean";

    int count = 0;
    private final HashMap<String,ActorRef> rics = new HashMap<>();

    public static class RicStoreRefs implements ControlMessage, Serializable
    {
        public static class RicActorRef implements Serializable
        {
            private final String ric;
            private final ActorRef ricStore;

            public RicActorRef(String ric, ActorRef ricStore) {
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
        private final LinkedList<RicActorRef> ricRefs;

        public RicStoreRefs(LinkedList<RicActorRef> ricRefs) {
            this.ricRefs = ricRefs;
        }

        // TODO: return immutable
        public Iterable<RicActorRef> getRicRefs() {
            return ricRefs;
        }
    }

    int idNext = 0;

    private static SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), throwable -> SupervisorStrategy.stop());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void onReceive(Object m) throws Exception
    {
        if (m instanceof TradeReal)
        {
            if (0 == (++count % (10*1000)))
            {
                System.out.println(">>> TradeRouter: Got " + count + " trades. Total of: " + rics.size() + " ric stores.");
            }

//            if (null != m)
//            {
//                return;
//            }

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
            //System.out.println(">>> TradeRouter(*): Got " + count + " trades. Total of: " + this.rics.size() + " ric stores.");

            LinkedList<RicStoreRefs.RicActorRef> ricRefs = new LinkedList<>();

            final Iterable<String> reqRics = ((StartCalcMultiRic) m).getRics();
            if (StartCalcMultiRic.isAllRics(reqRics))
            {
                for (Map.Entry<String,ActorRef> entry : this.rics.entrySet()) {
                    ricRefs.add(new RicStoreRefs.RicActorRef(entry.getKey(), entry.getValue()));
                }
            }
            else {
                for (String ric : reqRics) {
                    ActorRef ricStore = this.rics.get(ric);
                    if (null != ricStore) {
                        ricRefs.add(new RicStoreRefs.RicActorRef(ric, ricStore));
                    }
                }
            }

            sender().tell(new RicStoreRefs(ricRefs), self());
        }
        else if (m instanceof TestVisitor)
        {
            if (((TestVisitor) m).getVerb().toLowerCase().equals(TESTVERB_CLEAN_STORE))
            {
                for (Map.Entry<String,ActorRef> entry : this.rics.entrySet()) {
                    entry.getValue().tell(m, self());
                }
            }
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
