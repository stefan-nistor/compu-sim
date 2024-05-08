package ro.uaic.swqual.proc;

import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.mem.ReadableWriteableMemoryUnit;
import ro.uaic.swqual.mem.WriteableMemoryUnit;
import ro.uaic.swqual.model.operands.*;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class ProxyUnit<HardwareUnit extends MemoryUnit> extends DelegatingUnit {
    protected final List<Tuple3<HardwareUnit, Character, Predicate<Character>>> hardwareUnits = new ArrayList<>();

    public void registerHardwareUnit(
            HardwareUnit hardwareUnit,
            Character offset,
            Predicate<Character> addressSpaceValidator
    ) {
        hardwareUnits.add(Tuple.of(hardwareUnit, offset, addressSpaceValidator));
    }

    @Override public Parameter locate(Parameter directOrLocation) {
        if (!(directOrLocation instanceof MemoryLocation location)) {
            return directOrLocation;
        }

        var acceptingUnits = hardwareUnits.stream()
                .filter(unitOffsetValidatorTuple -> unitOffsetValidatorTuple.getThird().test(location.getValue()))
                .map(unitOffsetValidatorTuple -> unitOffsetValidatorTuple.map(
                        (first, second, third) -> Tuple.of((MemoryUnit) first, second, third)
                ))
                .collect(Collectors.toCollection(ArrayList::new));

        if (acceptingUnits.size() > 1) {
            raiseFlag(FlagRegister.MULTISTATE_FLAG);
            acceptingUnits.clear();
        }

        var fromDelegate = super.locate(location);
        if (acceptingUnits.isEmpty()) {
            if (fromDelegate instanceof ResolvedMemory) {
                return fromDelegate;
            }
            return unresolvedSink;
        }

        if (fromDelegate instanceof ResolvedMemory) {
            raiseFlag(FlagRegister.MULTISTATE_FLAG);
            return unresolvedSink;
        }

        var acceptingUnitOffsetTuple =
                acceptingUnits.getFirst().map((first, second, ignored) -> Tuple.of(first, second));
        var unit = acceptingUnitOffsetTuple.getFirst();
        var offset = acceptingUnitOffsetTuple.getSecond();
        var directLocation = new DirectMemoryLocation((char) (location.getValue() - offset));
        var discardingMemoryUnit = new ReadableWriteableMemoryUnit() {
            @Override public void write(MemoryLocation location, char value) {
                raiseFlag(FlagRegister.SEG_FLAG);
            }

            @Override public char read(MemoryLocation location) {
                raiseFlag(FlagRegister.SEG_FLAG);
                return 0;
            }
        };

        var readableMemoryUnit = (unit instanceof ReadableMemoryUnit readable)
                ? readable
                : discardingMemoryUnit;
        var writeableMemoryUnit = (unit instanceof WriteableMemoryUnit writeable)
                ? writeable
                : discardingMemoryUnit;
        return new ResolvedMemory(
                () -> readableMemoryUnit.read(directLocation),
                (value) -> writeableMemoryUnit.write(directLocation, value)
        );
    }
}
