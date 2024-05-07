package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.*;

import java.util.function.Predicate;

/**
 * Memory Management Unit
 * Its purpose is to handle data transfer operations between different storable locations
 */
public class MMU extends DelegatingUnit {
    final MemoryUnit primaryMemoryUnit;
    final AbsoluteMemoryLocation stackPointer;
    final FlagRegister flagRegister;

    public MMU(MemoryUnit primaryMemoryUnit, FlagRegister flagRegister, Register stackPointer) {
        this.primaryMemoryUnit = primaryMemoryUnit;
        this.flagRegister = flagRegister;
        this.stackPointer = new AbsoluteMemoryLocation(stackPointer);
    }

    private void mov(Parameter dst, Parameter src) {
        dst.setValue(src.getValue());
    }

    private void push(Parameter value, Parameter unused) {
        throw new UnsupportedOperationException();
    }

    private void pop(Parameter dest, Parameter unused) {
        throw new UnsupportedOperationException();
    }

    private void call(Parameter location, Parameter unused) {
        throw new UnsupportedOperationException();
    }

    private void ret(Parameter unused0, Parameter unused1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Parameter locate(Parameter writeableOrLocation) {
        if (writeableOrLocation instanceof MemoryLocation location) {
            if (location instanceof UndefinedMemoryLocation) {
                return new Parameter() {
                    @Override
                    public void setValue(char value) {
                        flagRegister.set(FlagRegister.SEG_FLAG);
                    }

                    @Override
                    public char getValue() {
                        flagRegister.set(FlagRegister.SEG_FLAG);
                        return 0;
                    }
                };
            }

            return new Parameter() {
                @Override
                public void setValue(char value) {
                    primaryMemoryUnit.write(location, value);
                }

                @Override
                public char getValue() {
                    return primaryMemoryUnit.read(location);
                }
            };
        }

        return writeableOrLocation;
    }

    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> instruction.getType().ordinal() >= InstructionType.MMU_MOV.ordinal()
                           && instruction.getType().ordinal() <= InstructionType.MMU_POP.ordinal();
    }

    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        // Do not use default execute, as we want to delegate on push, pop, call, ret
        switch (instruction.getType()) {
            case MMU_MOV -> mov(locate(instruction.getParam1()), locate(instruction.getParam2()));
            case MMU_POP -> pop(instruction.getParam1(), instruction.getParam2());
            case MMU_PUSH -> push(instruction.getParam1(), instruction.getParam2());
            case MMU_RET -> ret(instruction.getParam1(), instruction.getParam2());
            case MMU_CALL -> call(instruction.getParam1(), instruction.getParam2());
            default -> throw new InstructionException("Unsupported type: '" + instruction.getType() + "'");
        }
    }
}
