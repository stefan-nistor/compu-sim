package ro.uaic.swqual.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the kind of {@link Instruction}, equivalent to its assembly scope and operation.
 */
public enum InstructionType {
    /* MMU - Memory Management Unit - operations */
    
    /** mov writeableDst src; - allows copying values. */
    MMU_MOV("mov"),
    /** push src; - allows pushing values onto the stack. */
    MMU_PUSH("push"),
    /** pop [writeable]; - allows popping values onto the stack, and can optionally copy
     *  the read value to an optional parameter. */
    MMU_POP("pop"),

    /* ALU - Arithmetic Logic Unit - operations */
    
    /** add {out} op1 op2; - executes op1 + op2 and stores it in op1. */
    ALU_ADD("add"),
    /** sub {out} op1 op2; - executes op1 - op2 and stores it in op1. */
    ALU_SUB("sub"),
    /** umul {out} op1 op2; - executes unsigned op1 * op2 and stores it in op1.
     *  Stores overflowing output in the additional register. */
    ALU_UMUL("umul"),
    /** smul {out} op1 op2; - executes signed op1 * op2 and stores it in op1.
     *  Stores overflowing output in the additional register. */
    ALU_SMUL("smul"),
    /** udiv {out} op1 op2; - executes unsigned op1 / op2 and stores it in op1.
     *  Also stores op1 % op2 in the additional output register. */
    ALU_UDIV("udiv"),
    /** sdiv {out} op1 op2; - executes signed op1 / op2 and stores it in op1.
     *  Also stores op1 % op2 in the additional output register. */
    ALU_SDIV("sdiv"),
    /** and {out} op1 op2; - executes bitwise op1 & op2 and stores it in op1. */
    ALU_AND("and"),
    /** or {out} op1 op2; - executes bitwise op1 | op2 and stores it in op1. */
    ALU_OR("or"),
    /** xior {out} op1 op2; - executes bitwise op1 ^ op2 and stores it in op1. */
    ALU_XOR("xor"),
    /** shl {out} op1 op2; - executes shift left op1 << op2 and stores it in op1. */
    ALU_SHL("shl"),
    /** shr {out} op1 op2; - executes shift right op1 >> op2 and stores it in op1. */
    ALU_SHR("shr"),
    /** not {out} op1; - executes bitwise ~op1 and stores it in op1. */
    ALU_NOT("not"),
    /** cmp <op1> op2; - compares the two values and stores results in the flag register. */
    ALU_CMP("cmp"),

    /* IPU - Instruction Processing Unit - operations */
    /** jmp address; - jumps unconditionally to given address (usually a label). */
    IPU_JMP("jmp"),
    /** jeq address; - jumps to given address (usually a label) if last comparison result is equal. */
    IPU_JEQ("jeq"),
    /** jne address; - jumps to given address (usually a label) if last comparison result is not equal. */
    IPU_JNE("jne"),
    /** jlt address; - jumps to given address (usually a label) if last comparison result is less than. */
    IPU_JLT("jlt"),
    /** jle address; - jumps to given address (usually a label) if last comparison result is less than or equal. */
    IPU_JLE("jle"),
    /** jgt address; - jumps to given address (usually a label) if last comparison result is greater than. */
    IPU_JGT("jgt"),
    /** jge address; - jumps to given address (usually a label) if last comparison result is greater than or equal. */
    IPU_JGE("jge"),
    /** call address; - allows calling a function, by address (usually a label). Will push the return address beforehand. */
    IPU_CALL("call"),
    /** ret; - allows returning from a function. Will acquire the return address from the stack value that was pushed at call. */
    IPU_RET("ret"),

    /** Dummy Op, used in parsing jump point placeholders (labels). */
    LABEL("@");

    /** Assembly code representation of the instruction. */
    public final String label;

    /** Map linking Assembly code representation to actual InstructionType values */
    private static final Map<String, InstructionType> BY_LABEL = new HashMap<>();

    /**
     * Method used to validate whether an instruction is correspondent to a range of instructions - [begin, end].
     * Used to identify whether the instruction comes from that range.
     * @param inQuestion currently checked instruction type
     * @param begin instruction type range start, inclusive.
     * @param end instruction type range end, inclusive.
     * @return true if the instruction in question exists in the range, false otherwise.
     */
    static boolean isInRange(InstructionType inQuestion, InstructionType begin, InstructionType end) {
        assert inQuestion != null;
        assert begin != null;
        assert end != null;
        return begin.ordinal() <= inQuestion.ordinal() && inQuestion.ordinal() <= end.ordinal();
    }

    /**
     * Method used to verify whether received {@link InstructionType} corresponds to a
     *   {@link ro.uaic.swqual.proc.MemoryManagementUnit MemoryManagementUnit} instruction.
     * @param instruction currently checked instruction type
     * @return true if the instruction in question is a
     *   {@link ro.uaic.swqual.proc.MemoryManagementUnit MemoryManagementUnit} instruction, false otherwise
     */
    public static boolean isMmuInstruction(InstructionType instruction) {
        return isInRange(instruction, MMU_MOV, MMU_POP);
    }

    /**
     * Method used to verify whether received {@link InstructionType} corresponds to an
     *   {@link ro.uaic.swqual.proc.ArithmeticLogicUnit ArithmeticLogicUnit} instruction.
     * @param instruction currently checked instruction type
     * @return true if the instruction in question is an
     *   {@link ro.uaic.swqual.proc.ArithmeticLogicUnit ArithmeticLogicUnit} instruction, false otherwise
     */
    public static boolean isAluInstruction(InstructionType instruction) {
        return isInRange(instruction, ALU_ADD, ALU_CMP);
    }

    /**
     * Method used to verify whether received {@link InstructionType} corresponds to an
     *   {@link ro.uaic.swqual.proc.InstructionProcessingUnit InstructionProcessingUnit} instruction.
     * @param instruction currently checked instruction type
     * @return true if the instruction in question is an
     *   {@link ro.uaic.swqual.proc.InstructionProcessingUnit InstructionProcessingUnit} instruction, false otherwise
     */
    public static boolean isIpuInstruction(InstructionType instruction) {
        return isInRange(instruction, IPU_JMP, IPU_RET);
    }

    /**
     * Primary constructor
     * @param label the assembly code representation of the instruction type
     */
    InstructionType(String label) {
        this.label = label;
    }

    /**
     * Method used to acquire the {@link InstructionType} from a given assembly code representation.
     * @param label assembly code representation
     * @return identified {@link InstructionType}, null if none found.
     */
    public static InstructionType fromLabel(String label) {
        assert label != null;
        return BY_LABEL.get(label);
    }

    static {
        // Initialize the map linking assembly code representations to InstructionType values.
        for (var type : InstructionType.values()) {
            BY_LABEL.put(type.label, type);
        }
    }
}
