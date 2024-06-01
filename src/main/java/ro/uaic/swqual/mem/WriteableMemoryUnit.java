package ro.uaic.swqual.mem;

import ro.uaic.swqual.model.operands.MemoryLocation;

/**
 * Represents a container of multiple write-only byte values, identifiable by address.
 */
public interface WriteableMemoryUnit extends MemoryUnit {
    /**
     * Method used to write a value at a given address.
     * @param location address to store to.
     * @param value value to store at address.
     */
    void write(MemoryLocation location, char value);
}
