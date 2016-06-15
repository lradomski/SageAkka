package com.tr.analytics.sage.akka.data;


public class StartCalcSingleRic extends StartCalc {
    private final String ric;

    public StartCalcSingleRic(String calcName, String instanceName, int id, String ric) {
        this(calcName, instanceName, id, false, ric);
    }

    public StartCalcSingleRic(String calcName, String instanceName, int id, boolean isSnapshot, String ric) {
        super(calcName, instanceName, id, isSnapshot);
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StartCalcSingleRic)
        {
            StartCalcSingleRic other = (StartCalcSingleRic)obj;
            return this.ric.equals(other.ric) && super.equals(obj);
        }
        else return false;

    }
}
