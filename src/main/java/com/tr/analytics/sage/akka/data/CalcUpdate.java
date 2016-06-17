package com.tr.analytics.sage.akka.data;

import com.tr.analytics.sage.akka.common.Function_WithExceptions;
import com.tr.analytics.sage.akka.data.serializers.SageSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class CalcUpdate<T extends SageSerializable>  extends CalcUpdateCore implements DataHolder<T>,SageSerializable {
    final T data;

    public CalcUpdate(int id, T data) {
        super(id);
        this.data = data;

    }

    public CalcUpdate(ObjectInputStream ois, Function_WithExceptions<ObjectInputStream, T, IOException> dataCreator) throws IOException {
        super(ois);
        this.data = dataCreator.apply(ois);
    }


    public T getData() {
        return data;
    }

    @Override
    public String toStringCore() {
        return super.toStringCore() + ", data:" + data.toString();
    }

    @Override
    public boolean equals(Object o) {
        CalcUpdate<T> other = (CalcUpdate<T>)o;
        if (null != o)
        {
            return other.data.equals(this.data);
        }
        else return false;
    }

    @Override
    public void serialize(ObjectOutputStream oos) throws IOException {
        super.serialize(oos);
        getData().serialize(oos);
    }
}