package com.tr.analytics.sage.akka.data;

public class StopCalc extends VerbCalcCore {

    public StopCalc(int id)
    {
        super("?", "?", id, false);
    }

    public StopCalc(StartCalc req)
    {
        super(req.getCalcName(), req.getInstanceName(), req.getId(), req.isSnapshot());
    }
}
