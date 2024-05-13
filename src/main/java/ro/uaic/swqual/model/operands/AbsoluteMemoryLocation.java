package ro.uaic.swqual.model.operands;

import java.util.Map;

public class AbsoluteMemoryLocation extends MemoryLocation {
    private Parameter location;

    public AbsoluteMemoryLocation(Parameter parameter) {
        this.location = parameter;
    }

    @Override
    public char getValue() {
        return location.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbsoluteMemoryLocation that = (AbsoluteMemoryLocation) o;
        return location.getValue() == that.location.getValue();
    }

    @Override
    public void resolveInnerReferences(Map<String, Register> registerMap) {
        if (location instanceof RegisterReference ref) {
            var referee = registerMap.get(ref.getName());
            if (referee != null) {
                location = referee;
            }
        }
    }

    @Override
    public String toString() {
        return "[" + location + "] (=" + Integer.toString(location.getValue(), 16) + ")";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
