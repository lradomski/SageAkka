package com.tr.analytics.sage.akka.data;

import java.util.Iterator;
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



    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StartCalcMultiRic)
        {
            StartCalcMultiRic other = (StartCalcMultiRic)obj;

            Iterator<String> itThis = getRics().iterator();
            Iterator<String> itOther = other.getRics().iterator();

            while (itThis.hasNext() && itOther.hasNext())
            {
                if (!itThis.next().equals(itOther.next()))
                {
                    return false;
                }
            }

            return itThis.hasNext() == itOther.hasNext() && super.equals(other);
        }
        else return false;

    }

    public boolean isAllRics()
    {
        return isAllRics(rics);
    }

    public static boolean isAllRics(Iterable<String> rics)
    {
        Iterator<String> i = rics.iterator();
        return i.hasNext() && i.next().equals("*");
    }
}
