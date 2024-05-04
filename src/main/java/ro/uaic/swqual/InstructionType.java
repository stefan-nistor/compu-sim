package ro.uaic.swqual;

import java.util.HashMap;
import java.util.Map;

public enum InstructionType {
    // ALU Ops
    ALU_ADD("add"),
    ALU_SUB("sub"),
    ALU_MUL("mul"),
    ALU_DIV("div"),
    ALU_AND("and"),
    ALU_OR("or"),
    ALU_XOR("xor"),
    ALU_SHL("shl"),
    ALU_SHR("shr"),
    ALU_NOT("not"),
    ALU_CMP("cmp");

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
