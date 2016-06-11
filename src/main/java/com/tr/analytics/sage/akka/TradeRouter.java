package com.tr.analytics.sage.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.tr.analytics.sage.shard.engine.TradeReal;

import java.util.HashMap;

public class TradeRouter extends UntypedActor{
    public final static String NAME = "ric";

    private final HashMap<String,ActorRef> rics = new HashMap<>();

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
    }
}
