package com.tr.analytics.sage.akka.data;

public class TradeFactory {
    public static TradeReal pv(double price, int volume) {
        TradeReal trade = new TradeReal();
        trade.setPrice((float)price);
        trade.setPriceUSD((float)price);
        trade.setVolume((long)volume);
        trade.setAdjustmentFactor(1.0D);
        trade.setDayStart(0L);
        trade.setIntradayTime(0);
        trade.setRicCurrency((short)1);
        trade.setTradeCurrency((short)1);
        return trade;
    }

    public static TradeReal simple(int quoteId, double price, int volume) {
        TradeReal trade = pv(price, volume);
        trade.setQuoteId((long)quoteId);
        trade.setRic("RIC" + quoteId);
        return trade;
    }
}
