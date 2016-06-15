package com.tr.analytics.sage.akka.data.serializers;

import akka.serialization.JSerializer;
import com.tr.analytics.sage.akka.data.TradeTotals;

import java.io.*;

public class TradeTotalsSerializer extends JSerializer {

    // This is whether "fromBinary" requires a "clazz" or not
    @Override public boolean includeManifest() {
        return false;
    }

    // Pick a unique identifier for your Serializer,
    // you've got a couple of billions to choose from,
    // 0 - 16 is reserved by Akka itself
    @Override public int identifier() {
        return SerializerIds.TRADE_TOTALS_SERIALIZER;
    }

    // "toBinary" serializes the given object to an Array of Bytes
    @Override public byte[] toBinary(Object obj) {
        TradeTotals tt = (TradeTotals) obj;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream oos = new ObjectOutputStream(stream);
            tt.serialize(oos);
            oos.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] ret = stream.toByteArray();
        return ret;
    }

    // "fromBinary" deserializes the given array,
    // using the type hint (if any, see "includeManifest" above)
    @Override public Object fromBinaryJava(byte[] bytes,
                                           Class<?> clazz) {
        TradeTotals t = new TradeTotals();
        try {
            t.deserialize(new ObjectInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }
}
