package com.tr.analytics.sage.akka.data;

import java.util.List;


public class StartCalcSingleRic extends StartCalc {
    private final String ric;

    public StartCalcSingleRic(String calcName, String instanceName, int id, String ric) {
        super(calcName, instanceName, id);
        this.ric = ric;
    }

    public String getRic() {
        return ric;
    }

    @Override
    public String toStringCore() {
        return super.toStringCore() + "/" + ric + "";
    }

    public static StartCalcSingleRic fromFor(StartCalcMultiRic multiRic, int id, String ric)
    {
        return new StartCalcSingleRic(multiRic.getCalcName(), multiRic.getInstanceName(), id, ric);
    }
}
