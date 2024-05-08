package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

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
}
