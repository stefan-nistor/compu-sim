package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.DirectMemoryLocation;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.UnresolvedMemory;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;
import ro.uaic.swqual.util.Tuple3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class DelegatingUnit implements ProcessingUnit, LocatingUnit {
    protected final List<Tuple2<ProcessingUnit, Predicate<Instruction>>> executorUnits = new ArrayList<>();
    protected final List<Tuple3<LocatingUnit, Character, Predicate<Character>>> locatingUnits = new ArrayList<>();
    protected final UnresolvedMemory unresolvedSink;

    protected DelegatingUnit() {
        this.unresolvedSink = new UnresolvedMemory(() -> raiseFlag(FlagRegister.SEG_FLAG));
    }

    protected <AbstractUnit> Tuple2<AbstractUnit, Character> getUnitAndOffsetForLocation(
            List<Tuple3<AbstractUnit, Character, Predicate<Character>>> units,
            MemoryLocation location
    ) {
        var acceptingUnits = units.stream()
                .filter(unitOffsetValidatorTuple -> unitOffsetValidatorTuple.getThird().test(location.getValue()))
                .map(unitOffsetValidatorTuple -> unitOffsetValidatorTuple.map(
                        (first, second, discarded) -> Tuple.of(first, second)
                )).toList();

        if (acceptingUnits.size() > 1) {
            raiseFlag(FlagRegister.MULTISTATE_FLAG);
            return null;
        }

        if (acceptingUnits.isEmpty()) {
            return null;
        }

        return acceptingUnits.getFirst();
    }

    public void registerExecutor(ProcessingUnit unit, Predicate<Instruction> filter) {
        executorUnits.add(Tuple.of(unit, filter));
    }

    public void registerExecutor(ProcessingUnit unit) {
        executorUnits.add(Tuple.of(unit, unit.getDefaultFilter()));
    }

    public void registerLocator(
            LocatingUnit unit,
            Character offset,
            Predicate<Character> addressSpaceValidator
    ) {
        locatingUnits.add(Tuple.of(unit, offset, addressSpaceValidator));
    }

    public void registerLocator(
            LocatingUnit unit,
            Character offset,
            Character size
    ) {
        locatingUnits.add(Tuple.of(unit, offset, (location) -> location >= offset && location <= offset + size));
    }

    @Override
    public Parameter locate(Parameter parameterOrLocation) {
        if (!(parameterOrLocation instanceof MemoryLocation location)) {
            return parameterOrLocation;
        }

        var locatorAndOffset = getUnitAndOffsetForLocation(locatingUnits, location);
        if (locatorAndOffset == null) {
            return unresolvedSink;
        }

        var locator = locatorAndOffset.getFirst();
        var offset = locatorAndOffset.getSecond();
        var directLocation = new DirectMemoryLocation((char) (location.getValue() - offset));
        return locator.locate(directLocation);
    }

    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        executorUnits.stream()
                .filter(executorValidatorTuple -> executorValidatorTuple.getSecond().test(instruction))
                .map(Tuple2::getFirst)
                .forEach(unit -> unit.execute(instruction));
    }

    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return executorUnits.stream().map(Tuple2::getSecond).reduce(i -> true, Predicate::or);
    }
}
