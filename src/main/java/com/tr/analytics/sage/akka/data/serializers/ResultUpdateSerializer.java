package com.tr.analytics.sage.akka.data.serializers;

import akka.serialization.JSerializer;
import com.tr.analytics.sage.akka.common.Function_WithExceptions;
import com.tr.analytics.sage.akka.data.CalcResult;
import com.tr.analytics.sage.akka.data.CalcUpdate;
import com.tr.analytics.sage.akka.data.TradeTotals;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Function;

import static com.tr.analytics.sage.akka.data.serializers.SerializerIds.CALC_RESULT_UPDATE_SERIALIZER;

public class ResultUpdateSerializer extends JSerializer {

    public static final HashMap<String, SageSerializer> serializers = new HashMap<>();
    public static final LinkedList<Function<Object,String>> manifestSelectors = new LinkedList<>();

    public static final String RESULT_TRADE_TOTALS = "R-TT";
    public static final String UPDATE_TRADE_TOTALS = "U-TT";

    final Function_WithExceptions<ObjectInputStream, TradeTotals, IOException> creatorTradeTotals = ois -> new TradeTotals(ois);

    {
        {
            String key = UPDATE_TRADE_TOTALS;
            manifestSelectors.push(o -> o instanceof CalcUpdate<?> && ((CalcUpdate<?>) o).getData() instanceof TradeTotals ? key : null);
            serializers.put(key, new DataHolderSerializer<TradeTotals, CalcUpdate<TradeTotals>>(
                    (ois, dataCreator) -> new CalcUpdate<TradeTotals>(ois, dataCreator),
                    creatorTradeTotals
            ));
        }

        {
            String key = RESULT_TRADE_TOTALS;
            manifestSelectors.push(o -> o instanceof CalcResult<?> && ((CalcResult<?>) o).getData() instanceof TradeTotals ? key : null);
            serializers.put(key, new DataHolderSerializer<TradeTotals, CalcResult<TradeTotals>>(
                    (ois, dataCreator) -> new CalcResult<TradeTotals>(ois, dataCreator),
                    creatorTradeTotals
            ));
        }

    }

    @Override
    public int identifier() {
        return CALC_RESULT_UPDATE_SERIALIZER;
    }


    protected String manifest(Object o) {

        for (Function<Object,String> select : manifestSelectors)
        {
            String m = select.apply(o);
            if (null != m)
            {
                return m;
            }
        }

        assert(false);
        return null;
    }

    @Override public byte[] toBinary(Object obj) {
        SageSerializable result = (SageSerializable) obj;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream oos = new ObjectOutputStream(stream);
            String manifest = this.manifest(obj);
            oos.writeUTF(manifest);
            result.serialize(oos);
            oos.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] ret = stream.toByteArray();
        return ret;
    }

    @Override
    public boolean includeManifest() {
        return false;
    }


    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifestClass) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));

            String manifest = ois.readUTF();
            SageSerializer serializer = serializers.get(manifest);
            return serializer.deserialize(ois);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
