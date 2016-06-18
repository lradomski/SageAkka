package com.tr.analytics.sage.akka.data;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CalcResultCore implements Serializable {
    final int id;

    public CalcResultCore(int id) {
        this.id = id;
    }

    public CalcResultCore(ObjectInputStream ois) throws IOException {
        this.id = ois.readInt();
    }

    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeInt(id);
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
