package ro.uaic.swqual.model.operands;

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
}
