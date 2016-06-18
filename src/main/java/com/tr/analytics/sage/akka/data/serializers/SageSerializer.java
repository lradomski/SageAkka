package com.tr.analytics.sage.akka.data.serializers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface SageSerializer {
    void serialize(Object obj, ObjectOutputStream oos) throws IOException;
    Object deserialize(ObjectInputStream ois) throws IOException;
}
