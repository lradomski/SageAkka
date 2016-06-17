package com.tr.analytics.sage.akka.data;

import com.tr.analytics.sage.akka.data.serializers.SageSerializable;

public class TradeReal extends com.tr.analytics.sage.shard.TradeReal implements SageSerializable, Trade {
    public TradeReal()
    {
        super();
    }

    public TradeReal(com.tr.analytics.sage.shard.TradeReal other)
    {
        super(other);
    }
}
