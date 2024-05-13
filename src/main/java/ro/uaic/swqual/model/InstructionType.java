package ro.uaic.swqual.model;

import java.util.HashMap;
import java.util.Map;

public enum InstructionType {
    // MMU Ops
    MMU_MOV("mov"),
    MMU_PUSH("push"),
    MMU_POP("pop"),

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

    // IPU Ops
    IPU_JMP("jmp"),
    IPU_JEQ("jeq"),
    IPU_JNE("jne"),
    IPU_JLT("jlt"),
    IPU_JLE("jle"),
    IPU_JGT("jgt"),
    IPU_JGE("jge"),
    IPU_CALL("call"),
    IPU_RET("ret"),

    // Dummy Op
    LABEL("@");

    static boolean isInRange(InstructionType inQuestion, InstructionType begin, InstructionType end) {
        return begin.ordinal() <= inQuestion.ordinal() && inQuestion.ordinal() <= end.ordinal();
    }

    public static boolean isMmuInstruction(InstructionType instruction) {
        return isInRange(instruction, MMU_MOV, MMU_POP);
    }

    public static boolean isAluInstruction(InstructionType instruction) {
        return isInRange(instruction, ALU_ADD, ALU_CMP);
    }

    public static boolean isIpuInstruction(InstructionType instruction) {
        return isInRange(instruction, IPU_JMP, IPU_RET);
    }

    public final String label;

    private static final Map<String, InstructionType> BY_LABEL = new HashMap<>();

    InstructionType(String label) {
        this.label = label;
    }

    public static InstructionType fromLabel(String label) {
        return BY_LABEL.get(label);
    }

    static {
        for (var type : InstructionType.values()) {
            BY_LABEL.put(type.label, type);
        }
    }
}
