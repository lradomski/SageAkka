package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.CalcResult;
import com.tr.analytics.sage.akka.data.CalcUpdate;
import com.tr.analytics.sage.akka.data.TradeTotals;
import junit.framework.TestCase;

import java.io.IOException;

public class ResultUpdateSerializerTest extends TestCase {

    public void testResultTradeTotals() throws IOException {
        ResultUpdateSerializer serializer = new ResultUpdateSerializer();
        CalcResult<TradeTotals> message = new CalcResult<TradeTotals>(1, new TradeTotals(10.0, 100, 1000));

        byte[] buffer = serializer.toBinary(message);
        CalcResult<TradeTotals> messageCopy = (CalcResult<TradeTotals>)serializer.fromBinary(buffer);
        assertEquals(message, messageCopy);
    }

    public void testUpdateTradeTotals() {
        ResultUpdateSerializer serializer = new ResultUpdateSerializer();
        CalcUpdate<TradeTotals> message = new CalcUpdate<TradeTotals>(1, new TradeTotals(10.0, 100, 1000));

        byte[] buffer = serializer.toBinary(message);
        CalcUpdate<TradeTotals> messageCopy = (CalcUpdate<TradeTotals>)serializer.fromBinary(buffer);
        assertEquals(message, messageCopy);

    }

}