package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.model.operands.UnresolvedMemory;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnresolvedMemoryTest implements TestUtility, RegisterTestUtility, ProcTestUtility {
    @Test
    void unresolvedMemoryShouldCallbackOnRead() {
        var hasBeenCalled = new AtomicBoolean(false);
        var mem = new UnresolvedMemory(() -> hasBeenCalled.set(true));
        discard(mem.getValue());
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void unresolvedMemoryShouldCallbackOnWrite() {
        var hasBeenCalled = new AtomicBoolean(false);
        var mem = new UnresolvedMemory(() -> hasBeenCalled.set(true));
        mem.setValue((char) 0xFF);
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void unresolvedMemoryEqualsTest() {
        Runnable r0 = () -> {};
        Runnable r1 = () -> discard(0);
        assertTrue(equalsCoverageTest(
                new UnresolvedMemory(r0),
                new UnresolvedMemory(r0),
                new UnresolvedMemory(r1),
                _const((char) 0x10)
        ));
    }

    @Test
    void unresolvedMemoryHashCodeTest() {
        Runnable r0 = () -> {};
        Runnable r1 = () -> discard(0);
        assertEquals(
                new UnresolvedMemory(r0).hashCode(),
                new UnresolvedMemory(r0).hashCode()
        );
        assertNotEquals(
                new UnresolvedMemory(r0).hashCode(),
                new UnresolvedMemory(r1).hashCode()
        );
    }
}
