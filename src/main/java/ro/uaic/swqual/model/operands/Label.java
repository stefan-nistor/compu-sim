package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

import java.util.Objects;

/**
 * Represents a jump label. These should only appear in non-processed instructions.
 */
public class Label extends Parameter {
    /** Actual label name */
    private final String name;

    /**
     * Primary constructor from given label string
     * @param label string containing label text
     */
    public Label(String label) {
        assert label != null;
        this.name = label;
    }

    /**
     * Label name getter
     * @return label name
     */
    public String getName() {
        return name;
    }

    /**
     * Parameter value getter. Always throws, as it should not be accessed by any processing unit
     * @return never returns
     * @throws ParameterException always
     */
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
