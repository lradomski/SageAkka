package com.tr.analytics.sage.akka.data;


import akka.dispatch.ControlMessage;
import com.tr.analytics.sage.akka.data.serializers.SageSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SageIdentify implements Serializable, ControlMessage, SageSerializable
{
    final private int id;

    public SageIdentify(int id) {
        this.id = id;
    }

    public SageIdentify(ObjectInputStream ois) throws IOException {
        this.id = ois.readInt();
    }

    @Override
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeInt(getId());

    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return ((SageIdentify)obj).getId() == this.getId();
    }
}
