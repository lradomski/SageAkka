package com.tr.analytics.sage.akka.data;

import java.util.List;


public class StartCalcMultiRic extends StartCalc {
    private Iterable<String> rics;

    public StartCalcMultiRic(String calcName, String instanceName, int id, Iterable<String> rics) {
        super(calcName, instanceName, id);
        this.rics = rics;
    }

    public Iterable<String> getRics() {
        return rics;
    }

    @Override
    public String toStringCore() {
        return super.toStringCore() + "/" + rics + "";
    }


}
