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

    // Will be done in IPU since these require knowledge of the program counter
    // Logic will be IPU:
    //          call(addr):
    //                   --push(pc)-------> MMU --mov(sref, pc)--> RAM
    //                                          --add(sp, 2)-----> ALU
    //                   --mov(pc, addr)--> MMU
    //          ret:
    //                   --pop(pc)--------> MMU --mov(pc, sref)--> RAM
    //                                          --sub(sp, 2)-----> ALU
    //
    //
    // IPU_CALL
    // IPU_RET

    // Dummy Op
    LABEL("@");

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
