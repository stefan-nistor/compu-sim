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
        if (!super.equals(o)) return false;
        RegisterReference that = (RegisterReference) o;
        return referencedAtLine == that.referencedAtLine && Objects.equals(asmName, that.asmName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), asmName, referencedAtLine);
    }
}
