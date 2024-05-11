package ro.uaic.swqual.model.operands;

import java.util.Map;
import java.util.Objects;

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
    public int hashCode() {
        return Objects.hashCode(location);
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
}
