package com.tr.analytics.sage.akka.data.serializers;

import akka.serialization.JSerializer;
import akka.serialization.SerializerWithStringManifest;
import com.tr.analytics.sage.akka.common.Function_WithExceptions;
import com.tr.analytics.sage.akka.data.CalcResult;
import com.tr.analytics.sage.akka.data.CalcUpdate;
import com.tr.analytics.sage.akka.data.TradeTotals;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Function;

import static com.tr.analytics.sage.akka.data.serializers.SerializerIds.CALC_RESULT_UPDATE_SERIALIZER;

public class ResultUpdateSerializer extends SerializerWithStringManifest {

    public static final HashMap<String, JSerializer> serializers = new HashMap<>();
    public static final LinkedList<Function<Object,String>> manifestSelectors = new LinkedList<>();

    public static final String RESULT_TRADE_TOTALS = "R-TT";
    public static final String UPDATE_TRADE_TOTALS = "U-TT";

    final Function_WithExceptions<ObjectInputStream, TradeTotals, IOException> creatorTradeTotals = ois -> new TradeTotals(ois);

    {
        manifestSelectors.push(o -> o instanceof CalcUpdate<?> && ((CalcUpdate<?>)o).getData() instanceof TradeTotals ? UPDATE_TRADE_TOTALS : null);
        serializers.put(RESULT_TRADE_TOTALS, new DataHolderSerializer<TradeTotals, CalcUpdate<TradeTotals>>(
                (ois,dataCreator) -> new CalcUpdate<TradeTotals>(ois, dataCreator),
                 creatorTradeTotals
        ));

        manifestSelectors.push(o -> o instanceof CalcResult<?> && ((CalcResult<?>)o).getData() instanceof TradeTotals ? RESULT_TRADE_TOTALS : null);
        serializers.put(RESULT_TRADE_TOTALS, new DataHolderSerializer<TradeTotals, CalcResult<TradeTotals>>(
                (ois,dataCreator) -> new CalcResult<TradeTotals>(ois, dataCreator),
                 creatorTradeTotals
        ));

    }

    @Override
    public int identifier() {
        return CALC_RESULT_UPDATE_SERIALIZER;
    }

    @Override
    public String manifest(Object o) {

        for (Function<Object,String> select : manifestSelectors)
        {
            String m = select.apply(o);
            if (null != m)
            {
                return m;
            }
        }

        return null;
    }

    @Override public byte[] toBinary(Object obj) {
        SageSerializable result = (SageSerializable) obj;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream oos = new ObjectOutputStream(stream);
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
    public Object fromBinary(byte[] bytes, String manifest) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));

            JSerializer serializer = serializers.get(manifest);
            return serializer.fromBinary(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
