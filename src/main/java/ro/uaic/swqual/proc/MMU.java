package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.mem.ReadableWriteableMemoryUnit;
import ro.uaic.swqual.mem.WriteableMemoryUnit;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Memory Management Unit
 * Its purpose is to handle data transfer operations between different storable locations
 */
public class MMU extends DelegatingUnit {
    private final Map<MemoryUnit, Predicate<Parameter>> memoryUnits = new HashMap<>();
    private final AbsoluteMemoryLocation stackPointer;
    private final FlagRegister flagRegister;

    public MMU(FlagRegister flagRegister, Register stackPointer) {
        this.flagRegister = flagRegister;
        this.stackPointer = new AbsoluteMemoryLocation(stackPointer);
    }

    public void registerMemoryUnit(MemoryUnit memoryUnit, Predicate<Parameter> addressSpaceValidator) {
        memoryUnits.put(memoryUnit, addressSpaceValidator);
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
    public Parameter locate(Parameter directOrLocation) {
        var fromDelegate = super.locate(directOrLocation);
        if (!(fromDelegate instanceof UndefinedMemoryLocation)) {
            return fromDelegate;
        }

        if (!(directOrLocation instanceof MemoryLocation location)) {
            return directOrLocation;
        }

        if (location instanceof UndefinedMemoryLocation) {
            return new Parameter() {
                @Override public void setValue(char value) {
                    flagRegister.set(FlagRegister.SEG_FLAG);
                }

                @Override public char getValue() {
                    flagRegister.set(FlagRegister.SEG_FLAG);
                    return 0;
                }
            };
        }

        var discardingMemoryUnit = new ReadableWriteableMemoryUnit() {
            @Override public void write(MemoryLocation location, char value) {
                flagRegister.set(FlagRegister.SEG_FLAG);
            }

            @Override public char read(MemoryLocation location) {
                flagRegister.set(FlagRegister.SEG_FLAG);
                return 0;
            }
        };

        Function<Parameter, MemoryUnit> acquireMemoryUnitForLocation = addressSpaceLocation -> {
            var identifiedUnits = memoryUnits.entrySet().stream().filter(entry -> entry.getValue().test(addressSpaceLocation))
                    .limit(2).toList();
            if (identifiedUnits.size() == 1) {
                return identifiedUnits.getFirst().getKey();
            }

            if (identifiedUnits.size() > 1) {
                flagRegister.set(FlagRegister.MULTISTATE_FLAG);
            }

            return discardingMemoryUnit;
        };

        Function<Parameter, ReadableMemoryUnit> acquireReadableMemoryUnitForLocation = addressSpaceLocation ->
            Objects.requireNonNullElse(
                    (ReadableMemoryUnit) acquireMemoryUnitForLocation.apply(addressSpaceLocation),
                    discardingMemoryUnit
            );

        Function<Parameter, WriteableMemoryUnit> acquireWriteableMemoryUnitForLocation = addressSpaceLocation ->
            Objects.requireNonNullElse(
                    (WriteableMemoryUnit) acquireMemoryUnitForLocation.apply(addressSpaceLocation),
                    discardingMemoryUnit
            );

        var writeableMemoryUnit = acquireWriteableMemoryUnitForLocation.apply(location);
        var readableMemoryUnit = acquireReadableMemoryUnitForLocation.apply(location);
        return new Parameter() {
            @Override
            public void setValue(char value) {
                writeableMemoryUnit.write(location, value);
            }

            @Override
            public char getValue() {
                return readableMemoryUnit.read(location);
            }
        };
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
