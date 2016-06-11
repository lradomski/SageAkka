package com.tr.analytics.sage.akka.data;


import java.io.Serializable;

public class CalcUpdateCore implements Serializable {
    private final int id;

    public CalcUpdateCore(int id) {

        this.id = id;
    }

    public static CalcUpdateCore from(StartCalc c)
    {
        return new CalcUpdateCore(c.getId());
    }

    @Override
    public String toString() {
        return "CalcUpdate[" + toStringCore() + "]";
    }

    public String toStringCore() {
        return "id=" + id;
    }

    public int getId() {
        return id;
    }

}