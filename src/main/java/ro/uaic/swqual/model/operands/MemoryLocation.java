package ro.uaic.swqual.model.operands;

import java.util.Map;

public abstract class MemoryLocation extends Parameter {
    public abstract void resolveInnerReferences(Map<String, Register> registerMap);
}
