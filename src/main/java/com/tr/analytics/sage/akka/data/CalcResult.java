package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

public class CalcResult implements Serializable {
    private final int id;

    public CalcResult(int id) {

        this.id = id;
    }

    public static CalcResult from(StartCalc c)
    {
        return new CalcResult(c.getId());
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
