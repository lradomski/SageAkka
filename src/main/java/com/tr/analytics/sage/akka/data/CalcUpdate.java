package com.tr.analytics.sage.akka.data;

public class CalcUpdate<T>  extends CalcUpdateCore
{
    final T data;

    public CalcUpdate(int id, T data) {
        super(id);
        this.data = data;

    }

    public T getData() {
        return data;
    }

    @Override
    public String toStringCore() {
        return super.toStringCore() + ", data:" + data.toString();
    }}