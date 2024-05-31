package ro.uaic.swqual.model.operands;

import java.util.Map;

/**
 * Represents the address of a memory value identified directly by another parameter's value at the time of resolve.
 */
public class AbsoluteMemoryLocation extends MemoryLocation {
    /** Retains a pointer to the {@link Parameter} that will provide the address at resolve. */
    private Parameter location;

    /**
     * Primary constructor
     * @param parameter to use as the address value.
     */
    public AbsoluteMemoryLocation(Parameter parameter) {
        assert parameter != null;
        this.location = parameter;
    }

    /**
     * Getter for address.
     * @return Address currently indicated by the retained parameter.
     */
    @Override
    public char getValue() {
        assert location != null;
        return location.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbsoluteMemoryLocation that = (AbsoluteMemoryLocation) o;
        return location.getValue() == that.location.getValue();
    }

    /**
     * Method that allows resolving any {@link RegisterReference} to {@link Register}
     * that may be part of the expression resolving the memory address.
     * @param registerMap name to {@link Register} dictionary.
     */
    @Override
    public void resolveInnerReferences(Map<String, Register> registerMap) {
        assert registerMap != null;
        if (location instanceof RegisterReference ref) {
            var referee = registerMap.get(ref.getName());
            if (referee != null) {
                location = referee;
            }
        }
    }

    @Override
    public String toString() {
        assert location != null;
        return "[" + location + "] (=0x" + Integer.toString(location.getValue(), 16) + ")";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
