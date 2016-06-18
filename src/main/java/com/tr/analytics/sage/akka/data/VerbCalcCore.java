package com.tr.analytics.sage.akka.data;


import akka.dispatch.ControlMessage;
import com.tr.analytics.sage.akka.common.ActorUtils;
import com.tr.analytics.sage.akka.data.serializers.SageSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class VerbCalcCore implements ControlMessage, Serializable, SageSerializable {

    private final String calcName;
    private final String instanceName;
    private final int id;
    private final boolean isSnapshot;
    private final int uniqueId; // id unique within JVM instance

    private static AtomicInteger nextUniqueId = new AtomicInteger(0);

    public VerbCalcCore(String calcName, String instanceName, int id)
    {
        this(calcName, instanceName, id, false);
    }

    public VerbCalcCore(String calcName, String instanceName, int id, boolean isSnapshot) {
        this.calcName = calcName;
        this.instanceName = instanceName;
        this.id = id;
        this.isSnapshot = isSnapshot;
        this.uniqueId = nextUniqueId.getAndIncrement();
    }

    public VerbCalcCore(ObjectInputStream ois) throws IOException {
        this.calcName = ois.readUTF();
        this.instanceName = ois.readUTF();
        this.id = ois.readInt();
        this.isSnapshot = ois.readBoolean();
        this.uniqueId = nextUniqueId.getAndIncrement();
    }


    @Override
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeUTF(getCalcName());
        oos.writeUTF(getInstanceName());
        oos.writeInt(getId());
        oos.writeBoolean(isSnapshot());
    }


    public String getCalcName() {
        return calcName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public int getId() { return id; }

    public int getUniqueId() {
        return uniqueId;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    public String toStringCore()
    {
        return getCalcName() + "-" + getInstanceName() + "-" + id + (isSnapshot ? "-snapshot" : "-streaming");
    }

    public String toString()
    {
        return "[?(" + toStringCore() + ")]";
    }

    public String toActorName(int outerId)
    {
        //return ActorUtils.makeActorName("Calc" + Integer.toString(outerId) + "-" + calcName + "-" + instanceName + "-" + id);//.replaceAll("[^a-zA-Z0-9-_\\.\\*\\$\\+:@&=,!~']", "");
        return ActorUtils.makeActorName(getInstanceName()+ "-" + getUniqueId());
        //return ActorUtils.makeActorName("Calc" + Integer.toString(outerId) + "-" + this.toStringCore());//.replaceAll("[^a-zA-Z0-9-_\\.\\*\\$\\+:@&=,!~']", "");
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VerbCalcCore)
        {
            VerbCalcCore other = (VerbCalcCore)obj;
            return this.isSnapshot == other.isSnapshot && this.calcName.equals(other.calcName) && this.instanceName.equals(other.instanceName)  && this.id == other.id;
        }
        else return false;

    }
}
