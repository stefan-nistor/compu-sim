package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.UnresolvedMemory;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;
import ro.uaic.swqual.util.Tuple3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Represents an abstract processing unit capable of delegation by routing: <br/>
 *   - {@link Instruction} {@link ProcessingUnit#execute execute} requests to other registered
 *     {@link ProcessingUnit ProcessingUnits} based on their {@link ProcessingUnit#getDefaultFilter filter}. <br/>
 *   - {@link MemoryLocation} {@link LocatingUnit#locate locate} requests to other registered
 *     {@link LocatingUnit LocatingUnits} based on their accepted address space.
 *     The addressed passed to the delegate memory unit has the offset inside the delegator truncated. <br/>
 *   - {@link ClockListener#onTick onTick} requests to other registered {@link ClockListener ClockListeners}.
 */
public abstract class DelegatingUnit implements ProcessingUnit, LocatingUnit, ClockListener {
    /** List of Processing Units and associated instruction filters */
    protected final List<Tuple2<ProcessingUnit, Predicate<Instruction>>> executorUnits = new ArrayList<>();
    /** List of Locating Units, offset in current Unit and address space validator */
    protected final List<Tuple3<LocatingUnit, Character, Predicate<Character>>> locatingUnits = new ArrayList<>();
    /** Set of registered ClockListeners */
    protected final Set<ClockListener> clockListeners = new HashSet<>();
    /** Default sink for memory that could not be located. <br/>
     * Returned when requested locate of memory not in this unit's address space. */
    protected final UnresolvedMemory unresolvedSink;

    /**
     * Default constructor. <br/>
     * Initialize {@link DelegatingUnit#unresolvedSink} behavior to raise {@link FlagRegister#SEG_FLAG} upon read/write.
     */
    protected DelegatingUnit() {
        this.unresolvedSink = new UnresolvedMemory(() -> raiseFlag(FlagRegister.SEG_FLAG));
    }

    /**
     * Method used to acquire an addressable unit from a map of addressable units
     *   (such as {@link DelegatingUnit#locatingUnits}) that accepts a given location.
     * @param units the map of addressable units
     * @param location the address to identify which unit will accept
     * @return The addressable unit that can accept the address. Null if none found
     * @param <AbstractUnit> is the type of addressable unit. When used in the current class, it is {@link LocatingUnit}
     */
    protected <AbstractUnit> Tuple2<AbstractUnit, Character> getUnitAndOffsetForLocation(
            List<Tuple3<AbstractUnit, Character, Predicate<Character>>> units,
            MemoryLocation location
    ) {
        assert units != null;
        assert location != null;

        // Attempt to find a Locating Unit that accepts the requested address.
        var acceptingUnits = units.stream()
                .filter(unitOffsetValidatorTuple -> unitOffsetValidatorTuple.getThird().test(location.getValue()))
                .map(unitOffsetValidatorTuple -> unitOffsetValidatorTuple.map(
                        (first, second, discarded) -> Tuple.of(first, second)
                )).toList();

        // If more found, raise an error state and return null, as we cannot discern which of these is actually
        // the intended target
        if (acceptingUnits.size() > 1) {
            raiseFlag(FlagRegister.MULTISTATE_FLAG);
            return null;
        }

        // If none found, return null
        if (acceptingUnits.isEmpty()) {
            return null;
        }

        // Return the singular unit identified.
        return acceptingUnits.getFirst();
    }

    /**
     * Method used to register an {@link ProcessingUnit executor} with a custom {@link Instruction} filter.
     * @param unit the unit to be registered
     * @param filter the filter to be used when routing instructions
     */
    public void registerExecutor(ProcessingUnit unit, Predicate<Instruction> filter) {
        assert unit != null;
        assert filter != null;
        executorUnits.add(Tuple.of(unit, filter));
    }

    /**
     * Method used to register an {@link ProcessingUnit executor} with its default {@link Instruction} filter.
     * @param unit the unit to be registered
     */
    public void registerExecutor(ProcessingUnit unit) {
        executorUnits.add(Tuple.of(unit, unit.getDefaultFilter()));
    }

    /**
     * Method used to register a {@link LocatingUnit locator} with an offset and an address space validator.
     * @param unit the unit to be registered
     * @param offset the offset in the current delegating unit address space
     * @param addressSpaceValidator the predicate validating which address will be accepted, relative to the delegating
     *                              unit
     */
    public void registerLocator(
            LocatingUnit unit,
            Character offset,
            Predicate<Character> addressSpaceValidator
    ) {
        assert unit != null;
        assert offset != null;
        assert addressSpaceValidator != null;
        locatingUnits.add(Tuple.of(unit, offset, addressSpaceValidator));
    }

    /**
     * Method used to register a {@link LocatingUnit locator} with an offset and an address space size.
     *   Will define an address space validator predicate from the offset and size.
     * @param unit the unit to be registered
     * @param offset the offset in the current delegating unit address space
     * @param size the size of the locating unit's address space
     */
    public void registerLocator(
            LocatingUnit unit,
            Character offset,
            Character size
    ) {
        // ensure that the address space is bounded to end - 1 exclusive
        // (a read/write request will access two bytes at once)
        registerLocator(unit, offset, location -> location >= offset && location + 1 < offset + size);
    }

    /**
     * Method used to register a {@link LocatingUnit locator} at offset 0 and accepting all addresses.
     *   Should be used when registering only one {@link LocatingUnit locator}.
     * @param unit the unit to be registered
     */
    public void registerLocator(LocatingUnit unit) {
        locatingUnits.add(Tuple.of(unit, (char) 0, location -> true));
    }

    /**
     * Method used to register an object that could be a {@link ClockListener} to the
     *   {@link DelegatingUnit#clockListeners listener set}.
     * @param potentialListener object to be added to the listener set
     */
    protected void registerPotentialClockListener(Object potentialListener) {
        assert potentialListener != null;
        if (potentialListener instanceof ClockListener listener) {
            registerClockListener(listener);
        }
    }

    /**
     * Method used to register a {@link ClockListener} to the {@link DelegatingUnit#clockListeners listener set}.
     * @param listener object to be added to the listener set
     */
    public void registerClockListener(ClockListener listener) {
        assert listener != null;
        clockListeners.add(listener);
    }

    /**
     * Method used to locate memory values. <br/>
     * <br/>
     * Given the nature of the {@link DelegatingUnit}, it will look through the {@link DelegatingUnit#locatingUnits}
     *   list to locate the unit to delegate locating to. <br/>
     * If a {@link LocatingUnit delegator} is found, it will request a {@link LocatingUnit#locate locate} with
     *   the location relative to the identified {@link LocatingUnit delegator} (the offset will be subtracted). <br/>
     * <br/>
     * When not given a {@link ro.uaic.swqual.model.operands.MemoryLocation MemoryLocation}, it is expected to return
     *   the parameter received unchanged. <br/>
     * <br/>
     * When given a {@link ro.uaic.swqual.model.operands.MemoryLocation MemoryLocation}, it is expected to either: <br/>
     *   - Return a {@link ro.uaic.swqual.model.operands.ResolvedMemory ResolvedMemory}, if locating
     *     the address was successful. <br/>
     *   - Return an {@link ro.uaic.swqual.model.operands.UnresolvedMemory UnresolvedMemory}, if locating the address
     *     was unsuccessful. <br/>
     * @param parameterOrLocation parameter to locate a value from.
     * @return Identified memory location. If not given a
     *   {@link ro.uaic.swqual.model.operands.MemoryLocation MemoryLocation}, will return the parameter unchanged.
     */
    @Override
    public Parameter locate(Parameter parameterOrLocation) {
        // If given a non-MemoryLocation, return it unchanged.
        if (!(parameterOrLocation instanceof MemoryLocation location)) {
            return parameterOrLocation;
        }

        // Identify which LocatingUnit matches the requested location
        var locatorAndOffset = getUnitAndOffsetForLocation(locatingUnits, location);
        if (locatorAndOffset == null) {
            // If no exact match found, return the default UnresolvedMemory sink
            return unresolvedSink;
        }

        var locator = locatorAndOffset.getFirst();
        var offset = locatorAndOffset.getSecond();
        // Compute the address relative to the delegated unit and request locate from that one.
        var directLocation = new ConstantMemoryLocation((char) (location.getValue() - offset));
        return locator.locate(directLocation);
    }
    /**
     * Method used to execute a given instruction. <br/>
     * Given the nature of the {@link DelegatingUnit}, it will look through the {@link DelegatingUnit#executorUnits}
     *   list to locate the units to delegate execution to. <br/>
     * @param instruction instruction to execute.
     * @throws InstructionException when given instruction cannot or should not be processed by
     *   the current {@link ProcessingUnit}
     * @throws ParameterException when given instruction contains any invalid/incompatible
     *   {@link ro.uaic.swqual.model.operands.Parameter Parameter} values, such as
     *   {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}
     */
    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        assert instruction != null;
        executorUnits.stream()
                .filter(executorValidatorTuple -> executorValidatorTuple.getSecond().test(instruction))
                .map(Tuple2::getFirst)
                .forEach(unit -> unit.execute(instruction));
    }

    /**
     * Method used to acquire a filtering interface for instructions. Validates whether the current
     *   unit can execute an {@link Instruction}. <br/>
     * Given the nature of the {@link DelegatingUnit}, it will compose a predicate from the delegated
     *   {@link DelegatingUnit#executorUnits executor} filters that will accept an {@link Instruction} as long as
     *   any delegate would.
     * @return the functional interface used for validation.
     */
    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return executorUnits.stream().map(Tuple2::getSecond).reduce(i -> false, Predicate::or);
    }

    /**
     * Method to be called on each clock tick.
     * Given the nature of the {@link DelegatingUnit},
     * it will call {@link ClockListener#onTick onTick} for each listener.
     */
    @Override
    public void onTick() {
        clockListeners.forEach(ClockListener::onTick);
    }
}
