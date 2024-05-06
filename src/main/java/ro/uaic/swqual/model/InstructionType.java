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

    IPU_JMP("jmp"),
    IPU_JEQ("jeq"),
    IPU_JNE("jne"),
    IPU_JLT("jlt"),
    IPU_JLE("jle"),
    IPU_JGT("jgt"),
    IPU_JGE("jge"),

    LABEL("@");

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
