package com.tr.analytics.sage.akka.data.serializers;

import com.tr.analytics.sage.akka.data.StartCalcSingleRic;

public class StartCalcSingleRicSerializer extends SageDataSerializerBase<StartCalcSingleRic> {

    public StartCalcSingleRicSerializer()
    {
        super(ois -> new StartCalcSingleRic(ois));
    }

    @Override
    public int identifier() {
        return SerializerIds.START_CALC_SINGLE_RIC_SERIALIZER;
    }
}
