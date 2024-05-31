package ro.uaic.swqual.proc;

import ro.uaic.swqual.model.operands.Parameter;

/**
 * Represents the minimal interface for a Memory Locating Unit.
 * Used to acquire memory values by passing locations.
 */
public interface LocatingUnit {
    /**
     * Method used to raise an error via a flag value, present in
     *   {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister}.
     * @param value flag value to raise.
     */
    void raiseFlag(char value);

    /**
     * Method used to locate memory values. <br/>
     * When not given a {@link ro.uaic.swqual.model.operands.MemoryLocation MemoryLocation}, it is expected to return
     *   the parameter received unchanged. <br/>
     * When given a {@link ro.uaic.swqual.model.operands.MemoryLocation MemoryLocation}, it is expected to either: <br/>
     *   - Return a {@link ro.uaic.swqual.model.operands.ResolvedMemory ResolvedMemory}, if locating
     *     the address was successful. <br/>
     *   - Return an {@link ro.uaic.swqual.model.operands.UnresolvedMemory UnresolvedMemory}, if locating the address
     *     was unsuccessful. <br/>
     * @param parameterOrLocation parameter to locate a value from.
     * @return Identified memory location. If not given a
     *   {@link ro.uaic.swqual.model.operands.MemoryLocation MemoryLocation}, will return the parameter unchanged.
     */
    Parameter locate(Parameter parameterOrLocation);
}
