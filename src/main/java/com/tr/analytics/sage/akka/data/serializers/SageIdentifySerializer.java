package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.SageIdentify;

public class SageIdentifySerializer extends SageDataSerializerBase<SageIdentify> {
    public SageIdentifySerializer()
    {
        super(ois -> new SageIdentify(ois));
    }

    @Override
    public int identifier() {
        return SerializerIds.SAGE_IDENTIFY_SERIALIZER;
    }
}
