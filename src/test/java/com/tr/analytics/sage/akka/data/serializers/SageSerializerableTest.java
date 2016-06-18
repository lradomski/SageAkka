package com.tr.analytics.sage.akka.data.serializers;


import com.tr.analytics.sage.akka.data.SageIdentify;
import com.tr.analytics.sage.akka.data.StartCalc;
import com.tr.analytics.sage.akka.data.StartCalcMultiRic;
import com.tr.analytics.sage.akka.data.StartCalcSingleRic;

import java.util.Arrays;


public class SageSerializerableTest extends SerializerTestBase {
    public void testStartCalc()
    {
        StartCalcSerializer serializer = new StartCalcSerializer();
        StartCalc message = new StartCalc("Calc", "Instance", 1, true);

        coreTest(serializer, message);
    }

    public void testStartCalcSingleRic()
    {
        StartCalcSingleRicSerializer serializer = new StartCalcSingleRicSerializer();
        StartCalcSingleRic message = new StartCalcSingleRic("Calc", "Instance", 1, true, "RIC");

        coreTest(serializer, message);
    }

    public void testStartCalcMultiRic()
    {
        StartCalcMultiRicSerializer serializer = new StartCalcMultiRicSerializer();
        StartCalcMultiRic message = new StartCalcMultiRic("Calc", "Instance", 1, true, Arrays.asList("RIC1", "RIC2"));

        coreTest(serializer, message);
    }


    public void testSageIdentify()
    {
        SageIdentifySerializer serializer = new SageIdentifySerializer();
        SageIdentify message = new SageIdentify(1);

        coreTest(serializer, message);
    }

}
