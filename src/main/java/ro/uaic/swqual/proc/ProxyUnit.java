package ro.uaic.swqual.proc;

import ro.uaic.swqual.mem.ProxyMemoryUnit;
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

public abstract class ProxyUnit<HardwareUnit extends MemoryUnit> extends DelegatingUnit {
    protected final List<Tuple3<HardwareUnit, Character, Predicate<Character>>> hardwareUnits = new ArrayList<>();
    protected final ReadableWriteableMemoryUnit invalidReadWriteSink = new ProxyMemoryUnit(
            loc -> { raiseFlag(FlagRegister.SEG_FLAG); return (char) 0; },
            (loc, value) -> raiseFlag(FlagRegister.SEG_FLAG)
    );

    public void registerHardwareUnit(
            HardwareUnit hardwareUnit,
            Character offset,
            Predicate<Character> addressSpaceValidator
    ) {
        hardwareUnits.add(Tuple.of(hardwareUnit, offset, addressSpaceValidator));
    }

    @Override
    public Parameter locate(Parameter directOrLocation) {
        if (!(directOrLocation instanceof MemoryLocation location)) {
            return directOrLocation;
        }

        var localUnitAndOffset = getUnitAndOffsetForLocation(hardwareUnits, location);
        var fromDelegate = super.locate(location);
        if (localUnitAndOffset == null) {
            if (fromDelegate instanceof ResolvedMemory) {
                return fromDelegate;
            }
            return unresolvedSink;
        }

        if (fromDelegate instanceof ResolvedMemory) {
            raiseFlag(FlagRegister.MULTISTATE_FLAG);
            return unresolvedSink;
        }

        var unit = localUnitAndOffset.getFirst();
        var offset = localUnitAndOffset.getSecond();
        var directLocation = new DirectMemoryLocation((char) (location.getValue() - offset));
        var readableMemoryUnit = (unit instanceof ReadableMemoryUnit readable) ? readable : invalidReadWriteSink;
        var writeableMemoryUnit = (unit instanceof WriteableMemoryUnit writeable) ? writeable : invalidReadWriteSink;
        return new ResolvedMemory(
                () -> readableMemoryUnit.read(directLocation),
                (value) -> writeableMemoryUnit.write(directLocation, value)
        );
    }
}
