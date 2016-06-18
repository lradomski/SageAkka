package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.common.Function2_WithExceptions;
import com.tr.analytics.sage.akka.common.Function_WithExceptions;
import com.tr.analytics.sage.akka.data.DataHolder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DataHolderSerializer<Data extends SageSerializable, Holder extends DataHolder<Data> & SageSerializable>
        implements SageSerializer
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

    public void serialize(Object obj, ObjectOutputStream oos) throws IOException {
        Holder result = (Holder) obj;
        result.serialize(oos);
    }

    public Object deserialize(ObjectInputStream ois) throws IOException {
        return holderCreator.apply(ois, dataCreator);
    }
}
