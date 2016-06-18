package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.TradeTotals;

public class TradeTotalsSerializerTest extends SerializerTestBase {
    public void testSerde() throws Exception {
        TradeTotals tt = new TradeTotals(123.456, 123456, 654321);
        TradeTotalsSerializer serializer = new TradeTotalsSerializer();

        coreTest(serializer, tt);

    }

}