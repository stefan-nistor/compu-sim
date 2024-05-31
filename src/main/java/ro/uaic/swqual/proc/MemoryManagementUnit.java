package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.Parameter;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static ro.uaic.swqual.model.InstructionType.ALU_ADD;
import static ro.uaic.swqual.model.InstructionType.ALU_SUB;
import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;

/**
 * Represents the Unit managing with the {@link MemoryUnit MemoryUnits}.
 */
public class MemoryManagementUnit extends ProxyUnit<MemoryUnit> {
    /** {@link AbsoluteMemoryLocation} built on top of the Stack Pointer {@link Register}. Represents
     * the address of the stack head
     * (whereas the {@link CentralProcessingUnit#getStackPointer stackPointer} register
     * is the value of that address). */
    private final AbsoluteMemoryLocation stackHeadReference;
    /** Reference to the {@link FlagRegister} to raise errors to */
    private final FlagRegister flagRegister;

    /** Special prebuilt {@link Instruction}.
     *  Increments the {@link CentralProcessingUnit#getStackPointer stackPointer} by calling
     *  {@link InstructionType#ALU_ADD add} after each {@link InstructionType#MMU_PUSH push} */
    private final Instruction incrementStackPointer;
    /** Special prebuilt {@link Instruction}.
     *  Decrements the {@link CentralProcessingUnit#getStackPointer stackPointer} by calling
     *  {@link InstructionType#ALU_SUB sub} before each {@link InstructionType#MMU_POP pop} */
    private final Instruction decrementStackPointer;
    /** Additional validator used to avoid negative overflows of the
     *  {@link CentralProcessingUnit#getStackPointer stackPointer}.
     *  Will raise {@link FlagRegister#SEG_FLAG} before such an error would happen. */
    private final Supplier<Boolean> preDecrementStackPointer;
    /** Offset to increment/decrement the {@link CentralProcessingUnit#getStackPointer stackPointer} with. */
    private static final Constant STACK_POINTER_OFFSET_ON_CHANGE = new Constant((char) 2);

    /**
     * Primary constructor
     * @param flagRegister reference to the {@link FlagRegister} to be used for raising status and errors
     * @param stackPointer reference to the {@link Register} that acts as the stack pointer
     */
    public MemoryManagementUnit(FlagRegister flagRegister, Register stackPointer) {
        assert flagRegister != null;
        assert stackPointer != null;
        this.flagRegister = flagRegister;
        // pre-construct the relevant instructions, location of the stack head, and the negative overflow safeguard.
        stackHeadReference = new AbsoluteMemoryLocation(stackPointer);
        incrementStackPointer = new Instruction(ALU_ADD, stackPointer, STACK_POINTER_OFFSET_ON_CHANGE);
        decrementStackPointer = new Instruction(ALU_SUB, stackPointer, STACK_POINTER_OFFSET_ON_CHANGE);
        preDecrementStackPointer = () -> {
            if (stackPointer.getValue() < STACK_POINTER_OFFSET_ON_CHANGE.getValue()) {
                flagRegister.set(SEG_FLAG);
                return false;
            }
            return true;
        };
    }

    /**
     * Method used to raise an error via a flag value, present in
     *   {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister}.
     * @param value flag value to raise.
     */
    @Override
    public void raiseFlag(char value) {
        this.flagRegister.set(value);
    }

    /**
     * Method executing the {@link InstructionType#MMU_MOV mov} instruction.
     * @param dst parameter to write to
     * @param src parameter to read from
     */
    private void mov(Parameter dst, Parameter src) {
        assert dst != null;
        assert src != null;
        dst.setValue(src.getValue());
    }

    /**
     * Method executing the {@link InstructionType#MMU_PUSH push} instruction.
     * It will effectively use {@link InstructionType#MMU_MOV mov} to copy the value onto the stack head, followed by
     *   delegating the prebuilt {@link MemoryManagementUnit#incrementStackPointer} instruction. <br/>
     * In a default scenario, the pop prebuilt pop will route through {@link CentralProcessingUnit}.
     * @param value the value to push onto the stack.
     */
    private void push(Parameter value) {
        assert value != null;
        mov(locate(stackHeadReference), value);
        super.execute(incrementStackPointer);
    }

    /**
     * Method executing the {@link InstructionType#MMU_POP pop} instruction.
     * It will effectively execute the prebuilt {@link MemoryManagementUnit#decrementStackPointer} by delegation,
     *   followed by a {@link InstructionType#MMU_MOV mov} to copy the value from the stack head, if an output
     *   parameter was provided. <br/>
     * In a default scenario, the pop prebuilt pop will route through {@link CentralProcessingUnit}.
     * @param dest the writeable output parameter. Can be null, in which case, only the copy will not execute.
     */
    private void pop(Parameter dest) {
        if (preDecrementStackPointer.get()) {
            super.execute(decrementStackPointer);
            // pop may be invoked without a destination parameter, just to remove.
            // In this case, just do not store value
            if (dest != null) {
                mov(dest, locate(stackHeadReference));
            }
        }
    }

    /**
     * Default filter for instructions. Accepts instructions according to {@link InstructionType#isMmuInstruction}.
     * @return The filter interface in question.
     */
    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> InstructionType.isMmuInstruction(instruction.getType());
    }

    /**
     * Method used to execute a given instruction.
     * @param instruction instruction to execute.
     * @throws InstructionException when given instruction cannot or should not be processed by
     *   the current {@link ProcessingUnit}
     * @throws ParameterException when given instruction contains any invalid/incompatible
     *   {@link ro.uaic.swqual.model.operands.Parameter Parameter} values, such as
     *   {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}
     */
    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        assert instruction != null;
        // Do not use default execute, as we want to delegate on push, pop, call, ret
        switch (instruction.getType()) {
            case MMU_MOV -> mov(locate(instruction.getParam1()), locate(instruction.getParam2()));
            case MMU_POP -> pop(locate(instruction.getParam1()));
            case MMU_PUSH -> push(locate(instruction.getParam1()));
            default -> throw new InstructionException("Unsupported type: '" + instruction.getType() + "'");
        }
    }
}
