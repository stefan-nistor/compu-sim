package ro.uaic.swqual.mem;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ProxyMemoryUnitTest {
    @Test
    public void proxyMemoryUnitShouldCallReadCallbackOnRead() {
        var proxy = new ProxyMemoryUnit((loc) -> (char) 0x20, null);
        Assert.assertEquals(0x20, proxy.read(null));
    }

    @Test
    public void proxyMemoryUnitShouldCallWriteCallbackOnWrite() {
        AtomicInteger storage = new AtomicInteger();
        var proxy = new ProxyMemoryUnit(null, (loc, value) -> storage.set(value));
        proxy.write(null, (char) 1234);
        Assert.assertEquals(1234, storage.get());
    }
}
