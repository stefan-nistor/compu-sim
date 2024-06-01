package ro.uaic.swqual.proc;

import ro.uaic.swqual.mem.ProxyMemoryUnit;
import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.mem.ReadableWriteableMemoryUnit;
import ro.uaic.swqual.mem.WriteableMemoryUnit;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.ResolvedMemory;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents an abstract {@link DelegatingUnit} that is also capable of routing the requests to the Hardware.
 * @param <HardwareUnit> is the type of Hardware Unit this class is a Proxy for. These must be addressable.
 */
public abstract class ProxyUnit<HardwareUnit extends MemoryUnit> extends DelegatingUnit {
    /** List of Hardware Units, offset in current Unit and address space validator */
    protected final List<Tuple3<HardwareUnit, Character, Predicate<Character>>> hardwareUnits = new ArrayList<>();
    /** Sink Hardware Unit that raises {@link FlagRegister#SEG_FLAG} when
     *   {@link ReadableWriteableMemoryUnit#read read} or {@link ReadableWriteableMemoryUnit#write write}
     *   are requested <br/>
     * Is used when {@link ProxyUnit#locate} failed to identify any single matching
     *   {@link DelegatingUnit} or {@link HardwareUnit} that could resolve the requested address. */
    protected final ReadableWriteableMemoryUnit invalidReadWriteSink = new ProxyMemoryUnit(
            loc -> { raiseFlag(FlagRegister.SEG_FLAG); return (char) 0; },
            (loc, value) -> raiseFlag(FlagRegister.SEG_FLAG)
    );

    /**
     * Method used to register a hardware unit with an offset and an address space validator.
     *   This will route locate and, potentially, onTick requests to it.
     * @param hardwareUnit the unit to be registered
     * @param offset the offset in the current delegating unit address space
     * @param addressSpaceValidator the predicate validating which address will be accepted, relative to the delegating
     *                              unit
     */
    public void registerHardwareUnit(
            HardwareUnit hardwareUnit,
            Character offset,
            Predicate<Character> addressSpaceValidator
    ) {
        assert hardwareUnit != null;
        // We are unsure whether this hardware can accept clock ticks
        registerPotentialClockListener(hardwareUnit);
        hardwareUnits.add(Tuple.of(hardwareUnit, offset, addressSpaceValidator));
    }

    /**
     * Method used to register a hardware unit with an offset and an address space size.
     *   Will define an address space validator predicate from the offset and size.
     *   This will route locate and, potentially, onTick requests to it.
     * @param hardwareUnit the unit to be registered
     * @param offset the offset in the current delegating unit address space
     * @param size the size of the locating unit's address space
     */
    public void registerHardwareUnit(
            HardwareUnit hardwareUnit,
            Character offset,
            Character size
    ) {
        // ensure that the address space is bounded to end - 1 exclusive
        // (a read/write request will access two bytes at once)
        registerHardwareUnit(hardwareUnit, offset, location -> location >= offset && location + 1 < offset + size);
    }

    /**
     * Method used to locate memory values. <br/>
     * <br/>
     * Given the nature of the {@link ProxyUnit}, it will: <br/>
     *   - look through the {@link ProxyUnit#hardwareUnits} list to locate the hardware unit to
     *     delegate locating to. <br/>
     *   - request the default {@link DelegatingUnit#locate locate} from {@link DelegatingUnit}, as
     *     other higher level units might identify this address as well. <br/>
     * <br/>
     * If multiple or no resolvers are found, it will result in an
     *   {@link ro.uaic.swqual.model.operands.UnresolvedMemory UnresolvedMemory}. <br/>
     * <br/>
     * If a {@link LocatingUnit} delegator is found, it will request a {@link LocatingUnit#locate locate} with
     *   the location relative to the identified {@link LocatingUnit delegator} (the offset will be subtracted). <br/>
     * <br/>
     * If a {@link HardwareUnit} delegator is found, it will create a {@link ResolvedMemory} that will read/write to
     *   the unit at the time of access.
     * @param directOrLocation parameter to locate a value from.
     * @return Identified memory location. If not given a
     *   {@link ro.uaic.swqual.model.operands.MemoryLocation MemoryLocation}, will return the parameter unchanged.
     */
    @Override
    public Parameter locate(Parameter directOrLocation) {
        // If given a non-MemoryLocation, return it unchanged
        if (!(directOrLocation instanceof MemoryLocation location)) {
            return directOrLocation;
        }

        // Identify if a HardwareUnit matches the requested location
        var localUnitAndOffset = getUnitAndOffsetForLocation(hardwareUnits, location);
        // Identify if a delegated LocatingUnit matches the requested location
        var fromDelegate = super.locate(location);

        // Only resolve if a single match is found
        if (localUnitAndOffset == null) {
            if (fromDelegate instanceof ResolvedMemory) {
                // If Delegate returned ResolvedMemory and no HardwareUnit found, return the ResolvedMemory
                return fromDelegate;
            }
            // Failed to locate as no unit returned a ResolvedMemory
            return unresolvedSink;
        }

        if (fromDelegate instanceof ResolvedMemory) {
            // Failed to locate as both a delegated unit returned a ResolvedMemory and we have located a HardwareUnit
            // from which to create a ResolvedMemory. We cannot discern which to use
            raiseFlag(FlagRegister.MULTISTATE_FLAG);
            return unresolvedSink;
        }

        var unit = localUnitAndOffset.getFirst();
        var offset = localUnitAndOffset.getSecond();
        // Compute the address relative to the delegated unit and request locate from that one.
        var directLocation = new ConstantMemoryLocation((char) (location.getValue() - offset));
        // For missing read/write operations, ensure that invalid access results in
        // an error being signaled through the sink.
        var readableMemoryUnit = (unit instanceof ReadableMemoryUnit readable) ? readable : invalidReadWriteSink;
        var writeableMemoryUnit = (unit instanceof WriteableMemoryUnit writeable) ? writeable : invalidReadWriteSink;
        // Finally, return the resolved memory.
        return new ResolvedMemory(
                () -> readableMemoryUnit.read(directLocation),
                value -> writeableMemoryUnit.write(directLocation, value)
        );
    }
}
