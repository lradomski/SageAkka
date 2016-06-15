package com.tr.analytics.sage.akka.data;


import akka.dispatch.ControlMessage;
import com.tr.analytics.sage.akka.common.ActorUtils;

import java.io.Serializable;

public class VerbCalcCore implements ControlMessage, Serializable {

    private final String calcName;
    private final String instanceName;
    private final int id;
    private final boolean isSnapshot;

    public VerbCalcCore(String calcName, String instanceName, int id)
    {
        this(calcName, instanceName, id, false);
    }

    public VerbCalcCore(String calcName, String instanceName, int id, boolean isSnapshot) {
        this.calcName = calcName;
        this.instanceName = instanceName;
        this.id = id;
        this.isSnapshot = isSnapshot;
    }

    public String getCalcName() {
        return calcName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public int getId() { return id; }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    public String toStringCore()
    {
        return calcName + "-" + instanceName + "-" + id + (isSnapshot ? "-snapshot" : "-streaming");
    }

    public String toString()
    {
        return "[?(" + toStringCore() + ")]";
    }

    public String toActorName(int outerId)
    {
        return ActorUtils.makeActorName("Calc" + Integer.toString(outerId) + "-" + calcName + "-" + instanceName + "-" + id);//.replaceAll("[^a-zA-Z0-9-_\\.\\*\\$\\+:@&=,!~']", "");
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
