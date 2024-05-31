package ro.uaic.swqual.mem;

import ro.uaic.swqual.model.operands.MemoryLocation;

/**
 * Represents a container of multiple read-only byte values, identifiable by address.
 */
public interface ReadableMemoryUnit extends MemoryUnit {
    /**
     * Method used to read a value located at a given address.
     * @param location address of the value to read.
     * @return read value.
     */
    char read(MemoryLocation location);
}
