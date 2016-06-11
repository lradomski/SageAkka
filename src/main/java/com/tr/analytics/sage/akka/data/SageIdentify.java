package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

import akka.actor.ActorRef;
import akka.dispatch.ControlMessage;

public class SageIdentify implements Serializable, ControlMessage
{
    int id;

    public SageIdentify(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }


}
