package com.tr.analytics.sage.akka.data;

import com.tr.analytics.sage.akka.RicStore;
import com.tr.analytics.sage.api.Trade;

import java.io.Serializable;

public class TradeTotals implements Serializable
{
    private final double totalTurnover;
    private final long totalVolume;
    private final long totalCount;

    public TradeTotals()
    {
        this.totalTurnover = 0.0;
        this.totalVolume = 0;
        this.totalCount = 0;
    }

    public TradeTotals(double totalTurnover, long totalVolume, long totalCount) {
        this.totalTurnover = totalTurnover;
        this.totalVolume = totalVolume;
        this.totalCount = totalCount;
    }

    public static TradeTotals from(Trade trade)
    {
        return new TradeTotals(trade.getPrice()*trade.getVolume(), trade.getVolume(), 1);
    }

    public static TradeTotals from(RicStore.Trades trades)
    {
        double totalTurnover = 0.0;
        long totalVolume = 0;
        long totalCount = 0;

        for(Trade t : trades.getTrades())
        {
            totalTurnover += t.getPrice()*t.getVolume();
            totalVolume += t.getVolume();
            ++totalCount;
        }

        return new TradeTotals(totalTurnover, totalVolume, totalCount);
    }

    public TradeTotals makeUpdated(Trade trade)
    {
        return makeUpdated(from(trade));
    }

    public TradeTotals makeUpdated(TradeTotals add) {
        return new TradeTotals(getTotalTurnover() + add.getTotalTurnover(), getTotalVolume() + add.getTotalVolume(), getTotalCount() + add.getTotalCount());
    }

    public double getTotalTurnover() {
        return totalTurnover;
    }

    public long getTotalVolume() {
        return totalVolume;
    }

    public long getTotalCount() {
        return totalCount;
    }

    @Override
    public String toString() {
        return "[tt=" + Double.toString(getTotalCount()) + ", tv=" + Long.toString(getTotalVolume()) + ", tc=" + Long.toString(getTotalCount()) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TradeTotals)
        {
            TradeTotals other = (TradeTotals)o;
            return other.getTotalVolume() == this.getTotalVolume() && other.getTotalTurnover() == this.getTotalTurnover() && other.getTotalCount() == this.getTotalCount();
        }
        else return false;
    }
}
