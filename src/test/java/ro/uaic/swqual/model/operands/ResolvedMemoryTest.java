package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ResolvedMemoryTest {
    @Test
    public void resolvedMemoryShouldUseSupplierOnRead() {
        var mem = new ResolvedMemory(() -> (char) 10, null);
        Assert.assertEquals(10, mem.getValue());
    }

    @Test
    public void resolvedMemoryShouldUseConsumerOnWrite() {
        var storage = new AtomicInteger();
        var mem = new ResolvedMemory(null, storage::set);
        mem.setValue((char)20);
        Assert.assertEquals(20, storage.get());
    }
}
