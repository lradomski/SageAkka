package com.tr.analytics.sage.akka.data;


public class StartCalc {

    private final String calcName;
    private final String name;

    public StartCalc(String calcName, String instanceName) {
        this.calcName = calcName;
        this.name = instanceName;
    }

    public String getCalcName() {
        return calcName;
    }

    public String getName() {
        return name;
    }


}
