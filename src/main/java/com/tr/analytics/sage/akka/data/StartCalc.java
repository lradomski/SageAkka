package com.tr.analytics.sage.akka.data;


import com.tr.analytics.sage.akka.common.ActorUtils;

import java.io.IOException;
import java.io.ObjectInputStream;

public class StartCalc extends VerbCalcCore {


    public StartCalc(String calcName, String instanceName, int id)
    {
        this(calcName, instanceName, id, false);
    }

    public StartCalc(String calcName, String instanceName, int id, boolean isSnapshot) {
        super(calcName, instanceName, id, isSnapshot);
    }

    public StartCalc(ObjectInputStream ois) throws IOException {
        super(ois);
    }


    public String toString()
    {
        return "[Calc(" + toStringCore() + ")]";
    }

    public String toActorName(int outerId)
    {
        return ActorUtils.makeActorName("Calc" + Integer.toString(outerId) + "-" + getCalcName() + "-" + getInstanceName() + "-" + getId());//.replaceAll("[^a-zA-Z0-9-_\\.\\*\\$\\+:@&=,!~']", "");
    }

    public StopCalc makeStop()
    {
        return new StopCalc(this);
    }


    @Override
    public boolean equals(Object obj) {

        if (obj instanceof StartCalc)
        {
            return super.equals(obj);
        }
        else return false;

    }
}
