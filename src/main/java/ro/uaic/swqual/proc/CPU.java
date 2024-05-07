package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class CPU implements ProcessingUnit {
    private final List<Register> dataRegisters = new ArrayList<>();
    private final FlagRegister flagRegister = new FlagRegister();
    private final Register programCounter = new Register();

    private final Map<ProcessingUnit, Predicate<Instruction>> processingUnits = new HashMap<>();

    public CPU() {
        IntStream.range(0, 8).forEach(regIndex -> dataRegisters.add(new Register()));
    }

    public void registerUnit(ProcessingUnit unit, Predicate<Instruction> filter) {
        processingUnits.put(unit, filter);
    }

    public void registerUnit(ProcessingUnit unit) {
        registerUnit(unit, unit.getDefaultFilter());
    }

    public FlagRegister getFlagRegister() {
        return flagRegister;
    }

    public Register getProgramCounter() {
        return programCounter;
    }

    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        processingUnits.entrySet().stream()
                .filter(entry -> entry.getValue().test(instruction))
                .map(Map.Entry::getKey)
                .forEach(unit -> unit.execute(instruction));

    }

    public List<Register> getDataRegisters() {
        return dataRegisters;
    }
}
