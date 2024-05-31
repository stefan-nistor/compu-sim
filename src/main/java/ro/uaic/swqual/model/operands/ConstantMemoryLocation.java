package ro.uaic.swqual.model.operands;

import java.util.Map;

/**
 * Represents a memory value at a constant address
 */
public class ConstantMemoryLocation extends MemoryLocation {
    /**
     * Primary constructor
     * @param value address to resolve the memory to
     */
    public ConstantMemoryLocation(char value) {
        this.value = value;
    }

    /**
     * Method overridden as base requirement. Does not do anything, as there is nothing to resolve.
     * @param registerMap unused
     */
    @Override
    public void resolveInnerReferences(Map<String, Register> registerMap) {
        // nothing to resolve
    }

    @Override
    public String toString() {
        return "[0x" + Integer.toString(value, 16) + "]";
    }
}
