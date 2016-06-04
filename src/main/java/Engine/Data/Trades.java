package Engine.Data;

/**
 * Created by luke on 6/4/16.
 */
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
