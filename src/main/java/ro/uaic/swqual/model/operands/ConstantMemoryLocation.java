package ro.uaic.swqual.model.operands;

import java.util.Map;

public class ConstantMemoryLocation extends MemoryLocation {
    public ConstantMemoryLocation(char value) {
        this.value = value;
    }

    @Override
    public void resolveInnerReferences(Map<String, Register> registerMap) {
        // nothing to resolve
    }

    @Override
    public String toString() {
        return "[" + Integer.toString(value, 16) + "]";
    }
}
