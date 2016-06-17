package com.tr.analytics.sage.akka.data.serializers;

import akka.serialization.JSerializer;
import com.tr.analytics.sage.akka.common.Function2_WithExceptions;
import com.tr.analytics.sage.akka.common.Function_WithExceptions;
import com.tr.analytics.sage.akka.data.DataHolder;

import java.io.*;

import static com.tr.analytics.sage.akka.data.serializers.SerializerIds.CALC_RESULT_UPDATE_SERIALIZER;

public class DataHolderSerializer<Data extends SageSerializable, Holder extends DataHolder<Data> & SageSerializable>
        extends JSerializer
{
    final Function2_WithExceptions<ObjectInputStream, Function_WithExceptions<ObjectInputStream, Data, IOException>, Holder, IOException> holderCreator;
    final Function_WithExceptions<ObjectInputStream, Data, IOException> dataCreator;

    public DataHolderSerializer(
            Function2_WithExceptions<
                    ObjectInputStream, Function_WithExceptions<ObjectInputStream, Data, IOException>,
                    Holder, IOException> holderCreator,
            Function_WithExceptions<ObjectInputStream, Data, IOException> dataCreator
    )
    {
        this.holderCreator = holderCreator;
        this.dataCreator = dataCreator;
    }

    @Override
    public int identifier() {
        return CALC_RESULT_UPDATE_SERIALIZER;
    }

    @Override public byte[] toBinary(Object obj) {
        Holder result = (Holder) obj;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream oos = new ObjectOutputStream(stream);
            result.serialize(oos);
            oos.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] ret = stream.toByteArray();
        return ret;
    }

    @Override
    public boolean includeManifest() {
        return false;
    }

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return holderCreator.apply(ois, dataCreator);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
