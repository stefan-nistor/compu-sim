package ro.uaic.swqual.unit.operands;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.model.operands.UnresolvedMemory;

import java.util.concurrent.atomic.AtomicBoolean;

class UnresolvedMemoryTest implements TestUtility {
    @Test
    void unresolvedMemoryShouldCallbackOnRead() {
        var hasBeenCalled = new AtomicBoolean(false);
        var mem = new UnresolvedMemory(() -> hasBeenCalled.set(true));
        discard(mem.getValue());
        Assertions.assertTrue(hasBeenCalled.get());
    }

    @Test
    void unresolvedMemoryShouldCallbackOnWrite() {
        var hasBeenCalled = new AtomicBoolean(false);
        var mem = new UnresolvedMemory(() -> hasBeenCalled.set(true));
        mem.setValue((char) 0xFF);
        Assertions.assertTrue(hasBeenCalled.get());
    }
}
