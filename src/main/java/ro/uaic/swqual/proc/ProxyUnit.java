package ro.uaic.swqual.proc;

import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.mem.ReadableWriteableMemoryUnit;
import ro.uaic.swqual.mem.WriteableMemoryUnit;
import ro.uaic.swqual.model.operands.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ProxyUnit<HardwareUnit extends MemoryUnit> extends DelegatingUnit {
    protected final Map<HardwareUnit, Map.Entry<Constant, Predicate<Parameter>>> hardwareUnits = new HashMap<>();
    protected final FlagRegister flagRegister;

    protected ProxyUnit(FlagRegister flagRegister) {
        this.flagRegister = flagRegister;
    }

    public void registerHardwareUnit(
            HardwareUnit hardwareUnit,
            Constant offset,
            Predicate<Parameter> addressSpaceValidator
    ) {
        hardwareUnits.put(hardwareUnit, Map.entry(offset, addressSpaceValidator));
    }

    @Override public Parameter locate(Parameter directOrLocation) {
        if (!(directOrLocation instanceof MemoryLocation location)) {
            return directOrLocation;
        }

        if (location instanceof UndefinedMemoryLocation) {
            return new UnresolvedMemory(flagRegister);
        }

        var targetedLocation = new AbsoluteMemoryLocation(new Constant(directOrLocation.getValue()));
        var discardingMemoryUnit = new ReadableWriteableMemoryUnit() {
            @Override public void write(MemoryLocation location, char value) {
                flagRegister.set(FlagRegister.SEG_FLAG);
            }

            @Override public char read(MemoryLocation location) {
                flagRegister.set(FlagRegister.SEG_FLAG);
                return 0;
            }
        };

        Function<Parameter, Map.Entry<MemoryUnit, Constant>> acquireMemoryUnitForLocation = addressSpaceLocation -> {
            var identifiedUnits = hardwareUnits.entrySet().stream()
                    .filter(entry -> entry.getValue().getValue().test(addressSpaceLocation))
                    .limit(2).toList();
            if (identifiedUnits.size() == 1) {
                return Map.entry(identifiedUnits.getFirst().getKey(), identifiedUnits.getFirst().getValue().getKey());
            }

            if (identifiedUnits.size() > 1) {
                flagRegister.set(FlagRegister.MULTISTATE_FLAG);
            }

            return Map.entry(discardingMemoryUnit, new Constant((char) 0));
        };

        Function<Parameter,  Map.Entry<ReadableMemoryUnit, Constant>> acquireReadableMemoryUnitForLocation = addressSpaceLocation -> {
            var unitAndOffset = acquireMemoryUnitForLocation.apply(addressSpaceLocation);
            if (unitAndOffset.getKey() instanceof ReadableMemoryUnit readable) {
                return Map.entry(readable, unitAndOffset.getValue());
            }
            return Map.entry(discardingMemoryUnit, new Constant((char) 0));
        };

        Function<Parameter,  Map.Entry<WriteableMemoryUnit, Constant>> acquireWritableMemoryUnitForLocation = addressSpaceLocation -> {
            var unitAndOffset = acquireMemoryUnitForLocation.apply(addressSpaceLocation);
            if (unitAndOffset.getKey() instanceof WriteableMemoryUnit writeable) {
                return Map.entry(writeable, unitAndOffset.getValue());
            }
            return Map.entry(discardingMemoryUnit, new Constant((char) 0));
        };

        var writeableMemoryUnitAndOffset = acquireWritableMemoryUnitForLocation.apply(targetedLocation);
        var readableMemoryUnitAndOffset = acquireReadableMemoryUnitForLocation.apply(targetedLocation);

        var fromDelegate = super.locate(directOrLocation);
        if (fromDelegate instanceof ResolvedMemory && (writeableMemoryUnitAndOffset.getKey() != discardingMemoryUnit || readableMemoryUnitAndOffset.getKey() != discardingMemoryUnit)) {
            flagRegister.set(FlagRegister.MULTISTATE_FLAG);
            return new UnresolvedMemory(flagRegister);
        }

        if (fromDelegate instanceof ResolvedMemory) {
            return fromDelegate;
        }

        if (fromDelegate instanceof UndefinedMemoryLocation && readableMemoryUnitAndOffset.getKey() == discardingMemoryUnit && writeableMemoryUnitAndOffset.getKey() == discardingMemoryUnit) {
            return new UnresolvedMemory(flagRegister);
        }

        return new ResolvedMemory(
                () -> readableMemoryUnitAndOffset.getKey().read(new AbsoluteMemoryLocation(new Constant((char) (targetedLocation.getValue() - readableMemoryUnitAndOffset.getValue().getValue())))),
                value -> writeableMemoryUnitAndOffset.getKey().write(new AbsoluteMemoryLocation(new Constant((char) (targetedLocation.getValue() - readableMemoryUnitAndOffset.getValue().getValue()))), value)
        );
    }
}
