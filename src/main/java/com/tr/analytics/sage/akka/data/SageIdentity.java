package com.tr.analytics.sage.akka.data;


import akka.actor.ActorRef;
import akka.dispatch.ControlMessage;
import akka.serialization.Serialization;
import com.tr.analytics.sage.akka.data.serializers.SageSerializable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SageIdentity implements Serializable, ControlMessage, SageSerializable {
    final int id;
    final ActorRef ref;

    public SageIdentity(int id, ActorRef ref) {
        this.id = id;
        this.ref = ref;
    }

//    public SageIdentity(ObjectInputStream ois) throws IOException {
//        this.id = ois.readInt();
//        this.ref = JavaSerializer.currentSystem().
//    }


    @Override
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeInt(this.id);
        String refStr = Serialization.serializedActorPath(this.ref);
        oos.writeUTF(refStr);

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
