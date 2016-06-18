package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.StartCalcMultiRic;

public class StartCalcMultiRicSerializer extends SageDataSerializerBase<StartCalcMultiRic> {

    public StartCalcMultiRicSerializer()
    {
        super(ois -> new StartCalcMultiRic(ois));
    }

    @Override
    public int identifier() {
        return SerializerIds.START_CALC_MULTI_RIC_SERIALIZER;
    }
}
