package ro.uaic.swqual.mem;

import ro.uaic.swqual.model.operands.MemoryLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ProxyMemoryUnit implements ReadableWriteableMemoryUnit {
    private final Function<MemoryLocation, Character> onRead;
    private final BiConsumer<MemoryLocation, Character> onWrite;

    public ProxyMemoryUnit(Function<MemoryLocation, Character> onRead, BiConsumer<MemoryLocation, Character> onWrite) {
        this.onRead = onRead;
        this.onWrite = onWrite;
    }

    @Override public void write(MemoryLocation location, char value) {
        onWrite.accept(location, value);
    }

    @Override public char read(MemoryLocation location) {
        return onRead.apply(location);
    }
}
