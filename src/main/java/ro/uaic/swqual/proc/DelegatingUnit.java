package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.UndefinedMemoryLocation;

import java.util.*;
import java.util.function.Predicate;

public abstract class DelegatingUnit implements ProcessingUnit, LocatingUnit {
    protected final Map<ProcessingUnit, Predicate<Instruction>> executorUnits = new HashMap<>();
    protected final List<LocatingUnit> locatingUnits = new ArrayList<>();

    public void registerExecutor(ProcessingUnit unit, Predicate<Instruction> filter) {
        executorUnits.put(unit, filter);
    }

    public void registerExecutor(ProcessingUnit unit) {
        executorUnits.put(unit, unit.getDefaultFilter());
    }

    public void registerLocator(LocatingUnit unit) {
        locatingUnits.add(unit);
    }

    public Parameter locate(Parameter parameterOrLocation) {
        return locatingUnits.stream()
                .map(unit -> unit.locate(parameterOrLocation))
                .filter(Objects::nonNull)
                .findFirst().orElse(new UndefinedMemoryLocation());
    }

    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        executorUnits.entrySet().stream()
                .filter(entry -> entry.getValue().test(instruction))
                .map(Map.Entry::getKey)
                .forEach(unit -> unit.execute(instruction));
    }

    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return executorUnits.values().stream().reduce(i -> true, Predicate::or);
    }
}
