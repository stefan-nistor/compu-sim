package ro.uaic.swqual;

public enum InstructionType {
    // ALU Ops
    ALU_ADD("add"),
    ALU_SUB("sub"),
    ALU_MUL("mul"),
    ALU_DIV("div"),
    ALU_GT("gt"),
    ALU_LT("lt"),
    ALU_GE("ge"),
    ALU_LE("le"),
    ALU_EQ("eq"),
    ALU_NE("neq");

    public final String label;
    InstructionType(String label) {
        this.label = label;
    }
}
