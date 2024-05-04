package ro.uaic.swqual.operational;

import java.util.Map;

public class Add extends Instruction {

    private final String dest;
    private final String src;

    public Add(String destRegName, String srcRegName){
        dest = destRegName;
        src = srcRegName;
    }

    @Override
    public boolean execute(Map<String, Short> dataRegistries) {
        Short newDestVal = (short)(dataRegistries.get(dest) + dataRegistries.get(src));
        dataRegistries.put(dest, newDestVal);
        return true;
    }
}
