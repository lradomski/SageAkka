package com.tr.analytics.sage.akka.data;

import akka.dispatch.ControlMessage;

import java.io.Serializable;

public class TestVisitor implements ControlMessage, Serializable
{
    private final String verb;
    private final Object data;

    public TestVisitor(String verb) {
        this.verb = verb;
        this.data = null;
    }

    public TestVisitor(String verb, Object data) {
        this.verb = verb;
        this.data = data;
    }

    public String getVerb() {
        return verb;
    }

    public Object getData() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestVisitor)
        {
            TestVisitor other = (TestVisitor)obj;
            return getVerb() == other.getVerb() && getData() == other.getData();
        } else return false;
    }
}
