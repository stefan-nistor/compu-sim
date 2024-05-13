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
 * Memory Management Unit
 * Its purpose is to handle data transfer operations between different storable locations
 */
public class MemoryManagementUnit extends ProxyUnit<MemoryUnit> {
    private final AbsoluteMemoryLocation stackHeadReference;
    private final FlagRegister flagRegister;

    private final Instruction incrementStackPointer;
    private final Instruction decrementStackPointer;
    private final Supplier<Boolean> preDecrementStackPointer;
    private static final Constant STACK_POINTER_OFFSET_ON_CHANGE = new Constant((char) 2);

    public MemoryManagementUnit(FlagRegister flagRegister, Register stackPointer) {
        this.flagRegister = flagRegister;
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

    @Override
    public void raiseFlag(char value) {
        this.flagRegister.set(value);
    }

    private void mov(Parameter dst, Parameter src) {
        dst.setValue(src.getValue());
    }

    private void push(Parameter value) {
        mov(locate(stackHeadReference), value);
        super.execute(incrementStackPointer);
    }

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

    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> InstructionType.isMmuInstruction(instruction.getType());
    }

    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        // Do not use default execute, as we want to delegate on push, pop, call, ret
        switch (instruction.getType()) {
            case MMU_MOV -> mov(locate(instruction.getParam1()), locate(instruction.getParam2()));
            case MMU_POP -> pop(locate(instruction.getParam1()));
            case MMU_PUSH -> push(locate(instruction.getParam1()));
            default -> throw new InstructionException("Unsupported type: '" + instruction.getType() + "'");
        }
    }
}
