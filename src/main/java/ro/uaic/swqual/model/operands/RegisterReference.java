package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

import java.util.Objects;

/**
 * Represents a register reference by identifier. These should only appear in non-processed instructions.
 */
public class RegisterReference extends Parameter {
    /** Register assembly identifier */
    private final String asmName;
    /** Line appearance */
    private final int referencedAtLine;

    /**
     * Primary constructor
     * @param referencedAtLine line of the reference in a multi-line context.
     * @param asmName register assembly identifier.
     */
    public RegisterReference(int referencedAtLine, String asmName) {
        assert asmName != null;
        this.asmName = asmName;
        this.referencedAtLine = referencedAtLine;
    }

    /**
     * Register assembly name getter
     * @return name of the register
     */
    public String getName() {
        return asmName;
    }

    /**
     * Appearance line getter
     * @return line value
     */
    public int getReferencedAtLine() {
        return referencedAtLine;
    }

    /**
     * Parameter value getter. Always throws, as it should not be accessed by any processing unit
     * @return never returns
     * @throws ParameterException always
     */
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
