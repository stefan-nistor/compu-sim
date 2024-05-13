package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

import java.util.Objects;

public class Label extends Parameter {
    private final String name;

    public Label(String label) {
        this.name = label;
    }

    public String getName() {
        return name;
    }

    @Override public char getValue() throws ParameterException {
        throw new ParameterException("Label does not contain an actual numeric value");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return Objects.equals(name, label.name);
    }

    @Override
    public String toString() {
        return "label(" + name + ")";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
