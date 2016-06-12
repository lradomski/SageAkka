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

    @Override
    public boolean equals(Object obj) {
        CalcResult<T> other = (CalcResult<T>)obj;
        if (other != null)
        {
            return other.getId() == this.getId() && this.getData().equals(this.getData());
        }
        else return false;
    }
}
