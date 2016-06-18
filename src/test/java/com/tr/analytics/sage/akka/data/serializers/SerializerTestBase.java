package com.tr.analytics.sage.akka.data.serializers;

import akka.serialization.JSerializer;
import junit.framework.TestCase;

public class SerializerTestBase extends TestCase
{
    public void testBase()
    {}

    public <S extends JSerializer, M> void coreTest(S serializer, M message)
    {
        byte[] buffer = serializer.toBinary(message);
        M messageCopy = (M)serializer.fromBinary(buffer);
        assertEquals(message, messageCopy);
    }
}
