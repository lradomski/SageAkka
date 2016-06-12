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

    @Override
    public boolean equals(Object o) {
        if (o instanceof CalcUpdateCore)
        {
            CalcUpdateCore other = (CalcUpdateCore)o;
            return this.id == other.id;
        }
        else return false;
    }

    public String toStringCore() {
        return "id=" + id;
    }

    public int getId() {
        return id;
    }

}