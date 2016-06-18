package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.CalcResult;
import com.tr.analytics.sage.akka.data.CalcUpdate;
import com.tr.analytics.sage.akka.data.TradeTotals;

import java.io.IOException;

public class ResultUpdateSerializerTest extends SerializerTestBase {

    public void testResultTradeTotals() throws IOException {
        ResultUpdateSerializer serializer = new ResultUpdateSerializer();
        CalcResult<TradeTotals> message = new CalcResult<TradeTotals>(1, new TradeTotals(10.0, 100, 1000));

        coreTest(serializer, message);
    }

    public void testUpdateTradeTotals() {
        ResultUpdateSerializer serializer = new ResultUpdateSerializer();
        CalcUpdate<TradeTotals> message = new CalcUpdate<TradeTotals>(1, new TradeTotals(10.0, 100, 1000));

        coreTest(serializer, message);
    }

}