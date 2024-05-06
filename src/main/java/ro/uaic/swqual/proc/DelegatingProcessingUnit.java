package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public abstract class DelegatingProcessingUnit implements ProcessingUnit {
    protected final Map<ProcessingUnit, Predicate<Instruction>> executorUnits = new HashMap<>();

    public void registerExecutor(ProcessingUnit unit, Predicate<Instruction> filter) {
        executorUnits.put(unit, filter);
    }

    public void registerExecutor(ProcessingUnit unit) {
        executorUnits.put(unit, unit.getDefaultFilter());
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
