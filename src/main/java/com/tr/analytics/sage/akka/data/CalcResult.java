package com.tr.analytics.sage.akka.data;

public class CalcResult<T>  extends CalcResultCore
{
    final T data;

    public CalcResult(int id, T data) {
        super(id);
        this.data = data;

    }

    public T getData() {
        return data;
    }

    @Override
    public String toStringCore() {
        return super.toStringCore() + ", data:" + data.toString();
    }
}
