package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.TradeTotals;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class TradeTotalsSerializerTest extends TestCase {
    public void testSerde() throws Exception {
        TradeTotals tt = new TradeTotals(123.456, 123456, 654321);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(output);
        tt.serialize(oos);
        oos.close();

        byte[] buffer = output.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(buffer);

        ObjectInputStream ois = new ObjectInputStream(input);
        TradeTotals ttCopy = new TradeTotals(ois);
        assertEquals(tt, ttCopy);

    }

}