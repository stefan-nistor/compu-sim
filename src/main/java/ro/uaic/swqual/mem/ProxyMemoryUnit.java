package ro.uaic.swqual.mem;

import ro.uaic.swqual.model.operands.MemoryLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Proxy for a {@link ReadableWriteableMemoryUnit}, delegating the read and write to functional interfaces received
 * upon creation.
 */
public class ProxyMemoryUnit implements ReadableWriteableMemoryUnit {
    /** Read proxy interface */
    private final Function<MemoryLocation, Character> onRead;
    /** Write proxy interface */
    private final BiConsumer<MemoryLocation, Character> onWrite;

    /**
     * Primary constructor
     * @param onRead proxy given to be called upon read
     * @param onWrite proxy given to be called upon write
     */
    public ProxyMemoryUnit(Function<MemoryLocation, Character> onRead, BiConsumer<MemoryLocation, Character> onWrite) {
        this.onRead = onRead;
        this.onWrite = onWrite;
    }

    /**
     * Method used to write a value at a given address. Will invoke write proxy with given parameters.
     * @param location address to store to.
     * @param value value to store at address.
     */
    @Override public void write(MemoryLocation location, char value) {
        assert onWrite != null;
        onWrite.accept(location, value);
    }

    /**
     * Method used to read a value located at a given address. Will invoke read proxy with given parameters
     * @param location address of the value to read.
     * @return read value
     */
    @Override public char read(MemoryLocation location) {
        assert onRead != null;
        var memory = onRead.apply(location);
        assert memory != null;
        return memory;
    }
}
