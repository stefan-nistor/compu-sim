package ro.uaic.swqual.unit.mem;

import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.mem.ReadableWriteableMemoryUnit;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface MemTestUtility extends TestUtility {
    default MemoryUnit proxyRWMemoryUnit(
            Function<MemoryLocation, Character> mapper,
            BiConsumer<MemoryLocation, Character> consumer
    ) {
        return new ReadableWriteableMemoryUnit() {
            @Override
            public char read(MemoryLocation location) {
                return mapper.apply(location);
            }

            @Override
            public void write(MemoryLocation location, char value) {
                consumer.accept(location, value);
            }
        };
    }

    default AbsoluteMemoryLocation aloc(Parameter reg) {
        return new AbsoluteMemoryLocation(reg);
    }
    default ConstantMemoryLocation dloc(char value) { return new ConstantMemoryLocation(value); }
}
