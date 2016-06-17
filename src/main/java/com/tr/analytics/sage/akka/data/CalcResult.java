package com.tr.analytics.sage.akka.data;

import com.tr.analytics.sage.akka.common.Function_WithExceptions;
import com.tr.analytics.sage.akka.data.serializers.SageSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class CalcResult<T extends SageSerializable>  extends CalcResultCore implements DataHolder<T>, SageSerializable
{
    final T data;

    public CalcResult(int id, T data) {
        super(id);
        this.data = data;
    }

    public CalcResult(ObjectInputStream ois, Function_WithExceptions<ObjectInputStream,T, IOException> creator) throws IOException {
        super(ois);
        this.data = creator.apply(ois);
    }


    public T getData() {
        return data;
    }

    @Override
    public String toStringCore() {
        return super.toStringCore() + ", data:" + data.toString();
    }

    @Override
    public boolean equals(Object obj) {
        CalcResult<T> other = (CalcResult<T>)obj;
        if (other != null)
        {
            return other.getId() == this.getId() && this.getData().equals(this.getData());
        }
        else return false;
    }

    public void serialize(ObjectOutputStream oos) throws IOException {
        super.serialize(oos);
        getData().serialize(oos);
    }
}
