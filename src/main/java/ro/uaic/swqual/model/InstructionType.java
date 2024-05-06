package ro.uaic.swqual.model;

import java.util.HashMap;
import java.util.Map;

public enum InstructionType {
    // ALU Ops
    ALU_ADD("add"),
    ALU_SUB("sub"),
    ALU_UMUL("umul"),
    ALU_SMUL("smul"),
    ALU_UDIV("udiv"),
    ALU_SDIV("sdiv"),
    ALU_AND("and"),
    ALU_OR("or"),
    ALU_XOR("xor"),
    ALU_SHL("shl"),
    ALU_SHR("shr"),
    ALU_NOT("not"),
    ALU_CMP("cmp"),


    // Jump ops (TODO: to be completed)
    JMP("jmp"),
    JMP_EQ("jeq"),
    JMP_NEQ("jne"),
    JMP_GT("jgt"),
    JMP_LT("jlt"),

    // Label special ops
    LABEL("@");

    private static final Map<String, InstructionType> BY_LABEL = new HashMap<>();

    static {
        for(InstructionType type : InstructionType.values()) {
            BY_LABEL.put(type.label, type);
        }
    }

    public static InstructionType fromLabel(String label) {
        var value = BY_LABEL.get(label);
        if (value == null) {
            throw new IllegalArgumentException("Unknown instruction type: " + label);
        }
        return value;
    }

    public final String label;
    InstructionType(String label) {
        this.label = label;
    }
}
