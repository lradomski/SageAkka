package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.shard.engine.TradeReal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import akka.serialization.JSerializer;

public class TradeSerializer extends JSerializer {

    // This is whether "fromBinary" requires a "clazz" or not
    @Override public boolean includeManifest() {
        return false;
    }

    // Pick a unique identifier for your Serializer,
    // you've got a couple of billions to choose from,
    // 0 - 16 is reserved by Akka itself
    @Override public int identifier() {
        return SerializerIds.TRADE_SERIALIZER;
    }

    // "toBinary" serializes the given object to an Array of Bytes
    @Override public byte[] toBinary(Object obj) {
        TradeReal t = (TradeReal)obj;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream oos = new ObjectOutputStream(stream);
            t.serialize(oos);
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
        TradeReal t = new TradeReal();
        try {
            t.deserialize(new ObjectInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }
}
