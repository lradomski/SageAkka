package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

import akka.actor.ActorRef;
import akka.dispatch.ControlMessage;

public class SageIdentity implements Serializable, ControlMessage {
    int id;
    ActorRef ref;

    public SageIdentity(int id, ActorRef ref) {
        this.id = id;
        this.ref = ref;
    }

    public int getId() {
        return id;
    }

    public ActorRef getRef() {
        return ref;
    }

    public static SageIdentity from(SageIdentify m, ActorRef ref)
    {
        return new SageIdentity(m.getId(), ref);
    }
}
