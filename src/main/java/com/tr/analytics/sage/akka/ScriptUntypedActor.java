package com.tr.analytics.sage.akka;

import akka.actor.UntypedActor;

public abstract class ScriptUntypedActor extends UntypedActor {
    public final Object data;

    public ScriptUntypedActor(Object data) {
        this.data = data;
    }


    @Override
    public final void onReceive(Object message) throws Exception {
        onReceiveCore(message, data);
    }

    public abstract void onReceiveCore(Object message, Object data);
}
