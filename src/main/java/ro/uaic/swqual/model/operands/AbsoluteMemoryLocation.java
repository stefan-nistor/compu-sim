package ro.uaic.swqual.model.operands;

import java.util.Objects;

public class AbsoluteMemoryLocation extends MemoryLocation {
    final Parameter location;

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
}
