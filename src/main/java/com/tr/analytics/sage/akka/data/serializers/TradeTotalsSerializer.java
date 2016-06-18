package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.TradeTotals;

public class TradeTotalsSerializer extends SageDataSerializerBase<TradeTotals> {

    public TradeTotalsSerializer()
    {
        super(ois -> new TradeTotals(ois));
    }

    @Override public int identifier() {
        return SerializerIds.TRADE_TOTALS_SERIALIZER;
    }
}
