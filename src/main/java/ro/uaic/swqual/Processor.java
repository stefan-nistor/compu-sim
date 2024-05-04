package ro.uaic.swqual;

import ro.uaic.swqual.operational.Instruction;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class for Processor
 */
public class Processor {
    private Map<String, Short> dataRegistriesMap;
    private short stackPointer;
    private short programCounter;
    private boolean[] flags;
    private static final Map<String, Short> registryMap = new HashMap<>();

    private static Processor instance;
    private Processor() {
        this.dataRegistriesMap = new HashMap<>();
        String[] keys = {"AX", "BX", "CX", "DX", "SI", "DI", "SP", "BP"};
        for (String key : keys) {
            dataRegistriesMap.put(key, (short) 0);
        }
    }
    public static Processor getInstance() {
        if (instance == null) {
            instance = new Processor();
        }
        return instance;
    }

    public void execute(Instruction instruction) {
        instruction.execute(dataRegistriesMap);
    }

}
