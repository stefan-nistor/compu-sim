package ro.uaic.swqual;

import java.util.HashMap;
import java.util.Map;

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

    private static final Map<String, InstructionType> BY_LABEL = new HashMap<>();

    static {
        for(InstructionType type : InstructionType.values()) {
            BY_LABEL.put(type.label, type);
        }
    }

    public static InstructionType fromLabel(String label) {
        return BY_LABEL.get(label);
    }

    public final String label;
    InstructionType(String label) {
        this.label = label;
    }
}
