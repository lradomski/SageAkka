package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

import common.ActorUtils;
import akka.dispatch.ControlMessage;

public class StartCalc implements ControlMessage, Serializable {

    private final String calcName;
    private final String instanceName;
    private final int id;

    public StartCalc(String calcName, String instanceName, int id) {
        this.calcName = calcName;
        this.instanceName = instanceName;
        this.id = id;
    }

    public String getCalcName() {
        return calcName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public int getId() { return id; }

    public String toStringCore()
    {
        return calcName + "-" + instanceName + "-" + id;
    }

    public String toString()
    {
        return "[Calc(" + toStringCore() + ")]";
    }

    public String toActorName(int outerId)
    {
        return ActorUtils.makeActorName("Calc" + Integer.toString(outerId) + "-" + this.toStringCore());//.replaceAll("[^a-zA-Z0-9-_\\.\\*\\$\\+:@&=,!~']", "");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StartCalc)
        {
            StartCalc other = (StartCalc)obj;
            return this.calcName.equals(other.calcName) && this.instanceName.equals(other.instanceName)  && this.id == other.id;
        }
        else return false;

    }
}
