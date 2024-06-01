package ro.uaic.swqual.model.operands;

import java.util.Map;

/**
 * Represents a parameter that refers to a value at a given memory address that has to be resolved.
 */
public abstract class MemoryLocation extends Parameter {
    /**
     * Method that allows resolving any {@link RegisterReference} to {@link Register}
     * that may be part of the expression resolving the memory address.
     * @param registerMap name to {@link Register} dictionary.
     */
    public abstract void resolveInnerReferences(Map<String, Register> registerMap);
}
