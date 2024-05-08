package ro.uaic.swqual.unit.operands;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.operands.ResolvedMemory;

import java.util.concurrent.atomic.AtomicInteger;

class ResolvedMemoryTest {
    @Test
    void resolvedMemoryShouldUseSupplierOnRead() {
        var mem = new ResolvedMemory(() -> (char) 10, null);
        Assertions.assertEquals(10, mem.getValue());
    }

    @Test
    void resolvedMemoryShouldUseConsumerOnWrite() {
        var storage = new AtomicInteger();
        var mem = new ResolvedMemory(null, storage::set);
        mem.setValue((char)20);
        Assertions.assertEquals(20, storage.get());
    }
}
