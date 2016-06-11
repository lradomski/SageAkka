package Engine.Data;

import com.tr.analytics.sage.api.Trade;


public class Trades {

    private Trade[] trades;
    private int endExclusive;

    public Trades(Trade[] trades, int endExclusive)
    {
        this.trades = trades;
        this.endExclusive = endExclusive;
    }

    public Trade[] getTrades() {
        return trades;
    }

    public int getEndExclusive() {
        return endExclusive;
    }
}
