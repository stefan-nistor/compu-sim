package ro.uaic.swqual.mem;

import ro.uaic.swqual.model.operands.MemoryLocation;

public interface WriteableMemoryUnit extends MemoryUnit {
    void write(MemoryLocation location, char value);
}
