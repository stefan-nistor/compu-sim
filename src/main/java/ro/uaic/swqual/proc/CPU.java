package ro.uaic.swqual.proc;

import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class CPU extends DelegatingProcessingUnit {
    private final List<Register> dataRegisters = new ArrayList<>();

    // Special purpose registers
    private final FlagRegister flagRegister = new FlagRegister();
    private final Register programCounter = new Register();
    private final Register stackPointer = new Register();

    public final Map<String, Register> registryReferenceMap = new HashMap<>();

    private final Map<ProcessingUnit, Predicate<Instruction>> processingUnits = new HashMap<>();

    public CPU() {
        IntStream.range(0, 8).forEach(regIndex -> {
            dataRegisters.add(new Register());
            registryReferenceMap.put("r" + regIndex, dataRegisters.getLast());
        });
    }

    public FlagRegister getFlagRegister() {
        return flagRegister;
    }

    public Register getProgramCounter() {
        return programCounter;
    }

    public Register getStackPointer() {
        return stackPointer;
    }

    public List<Register> getDataRegisters() {
        return dataRegisters;
    }
}
