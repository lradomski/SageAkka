package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

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
        return calcName + "/" + instanceName + "/" + id;
    }

    public String toString()
    {
        return "[Calc(" + toStringCore() + ")]";
    }

}
