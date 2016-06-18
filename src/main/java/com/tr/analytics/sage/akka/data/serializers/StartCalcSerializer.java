package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.StartCalc;

public class StartCalcSerializer extends SageDataSerializerBase<StartCalc> {

    public StartCalcSerializer()
    {
        super(ois -> new StartCalc(ois));
    }

    @Override public int identifier() {
        return SerializerIds.START_CALC_SERIALIZER;
    }
}
