package com.tr.analytics.sage.akka.data.serializers;

import java.io.IOException;
import java.io.ObjectOutputStream;


public interface SageSerializable {
    void serialize(ObjectOutputStream oos) throws IOException;
    // deserialize is meant to be handled via ctor(ois) - see CalcResult/Update
}
