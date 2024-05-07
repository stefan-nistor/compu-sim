package ro.uaic.swqual.mem;

import ro.uaic.swqual.model.operands.MemoryLocation;

public interface MemoryUnit {
    char read(MemoryLocation location);
    void write(MemoryLocation location, char value);
}
