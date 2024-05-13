package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

import java.util.Objects;

public class RegisterReference extends Parameter {
    private final String asmName;
    private final int referencedAtLine;

    public RegisterReference(int referencedAtLine, String asmName) {
        this.asmName = asmName;
        this.referencedAtLine = referencedAtLine;
    }

    public String getName() {
        return asmName;
    }

    public int getReferencedAtLine() {
        return referencedAtLine;
    }

    @Override
    public char getValue() throws ParameterException {
        throw new ParameterException("Label does not contain an actual numeric value");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterReference that = (RegisterReference) o;
        return referencedAtLine == that.referencedAtLine && Objects.equals(asmName, that.asmName);
    }

    @Override
    public String toString() {
        return "ref(" + asmName + ")";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
