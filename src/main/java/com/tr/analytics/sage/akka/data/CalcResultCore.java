package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

public class CalcResultCore implements Serializable {
    private final int id;

    public CalcResultCore(int id) {

        this.id = id;
    }

    public static CalcResultCore from(StartCalc c)
    {
        return new CalcResultCore(c.getId());
    }

    @Override
    public String toString() {
        return "CalcResult[" + toStringCore() + "]";
    }

    public String toStringCore() {
        return "id=" + id;
    }

    public int getId() {
        return id;
    }

}
