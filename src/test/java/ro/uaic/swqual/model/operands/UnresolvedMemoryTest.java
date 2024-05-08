package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.TestUtility;

import java.util.concurrent.atomic.AtomicBoolean;

public class UnresolvedMemoryTest implements TestUtility {
    @Test public void unresolvedMemoryShouldCallbackOnRead() {
        var hasBeenCalled = new AtomicBoolean(false);
        var mem = new UnresolvedMemory(() -> hasBeenCalled.set(true));
        discard(mem.getValue());
        Assert.assertTrue(hasBeenCalled.get());
    }

    @Test public void unresolvedMemoryShouldCallbackOnWrite() {
        var hasBeenCalled = new AtomicBoolean(false);
        var mem = new UnresolvedMemory(() -> hasBeenCalled.set(true));
        mem.setValue((char) 0xFF);
        Assert.assertTrue(hasBeenCalled.get());
    }
}
