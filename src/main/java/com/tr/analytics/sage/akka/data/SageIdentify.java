package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

import akka.actor.ActorRef;

public class SageIdentify implements Serializable
{
    int id;

    public SageIdentify(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }


}
