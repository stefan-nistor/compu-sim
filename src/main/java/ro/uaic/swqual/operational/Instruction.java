package ro.uaic.swqual.operational;

import java.util.Map;

public abstract class Instruction {

    public abstract boolean execute(Map<String, Short> dataRegistries);
}
