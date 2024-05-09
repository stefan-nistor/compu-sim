package ro.uaic.swqual.unit.mem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.mem.ProxyMemoryUnit;

import java.util.concurrent.atomic.AtomicInteger;

class ProxyMemoryUnitTest {
    @Test
    void proxyMemoryUnitShouldCallReadCallbackOnRead() {
        var proxy = new ProxyMemoryUnit((loc) -> (char) 0x20, null);
        Assertions.assertEquals(0x20, proxy.read(null));
    }

    @Test
    void proxyMemoryUnitShouldCallWriteCallbackOnWrite() {
        AtomicInteger storage = new AtomicInteger();
        var proxy = new ProxyMemoryUnit(null, (loc, value) -> storage.set(value));
        proxy.write(null, (char) 1234);
        Assertions.assertEquals(1234, storage.get());
    }
}
