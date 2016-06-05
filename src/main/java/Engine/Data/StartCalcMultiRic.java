package Engine.Data;

import java.util.Iterator;
import java.util.List;


public class StartCalcMultiRic extends StartCalc {
    private List<String> rics;

    public StartCalcMultiRic(String calcName, String instanceName, List<String> rics) {
        super(calcName, instanceName);
    }

    public Iterable<String> getRics() {
        return rics;
    }
}
