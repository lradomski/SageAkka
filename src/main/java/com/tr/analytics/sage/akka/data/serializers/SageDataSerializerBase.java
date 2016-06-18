package com.tr.analytics.sage.akka.data.serializers;

import akka.serialization.JSerializer;
import com.tr.analytics.sage.akka.common.Function_WithExceptions;

import java.io.*;

public abstract class SageDataSerializerBase<T extends SageSerializable> extends JSerializer {

    final Function_WithExceptions<ObjectInputStream,T,IOException> creator;
    public SageDataSerializerBase(Function_WithExceptions<ObjectInputStream,T,IOException> creator)
    {
        this.creator = creator;
    }

    // This is whether "fromBinary" requires a "clazz" or not
    @Override public boolean includeManifest() {
        return false;
    }

    // Pick a unique identifier for your Serializer,
    // you've got a couple of billions to choose from,
    // 0 - 16 is reserved by Akka itself
    @Override public abstract int identifier();

    // "toBinary" serializes the given object to an Array of Bytes
    @Override public byte[] toBinary(Object obj) {
        T t = (T) obj;
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
    @Override public Object fromBinaryJava(byte[] bytes, Class<?> clazz) {
        try {
            return creator.apply(new ObjectInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
