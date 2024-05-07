package ro.uaic.swqual.mem;

import ro.uaic.swqual.model.operands.MemoryLocation;

public interface ReadableMemoryUnit extends MemoryUnit {
    char read(MemoryLocation location);
}
