package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.operands.ResolvedMemory;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolvedMemoryTest implements RegisterTestUtility, TestUtility, ProcTestUtility {
    @Test
    void resolvedMemoryShouldUseSupplierOnRead() {
        var mem = new ResolvedMemory(() -> (char) 10, null);
        assertEquals(10, mem.getValue());
    }

    @Test
    void resolvedMemoryShouldUseConsumerOnWrite() {
        var storage = new AtomicInteger();
        var mem = new ResolvedMemory(null, storage::set);
        mem.setValue((char)20);
        assertEquals(20, storage.get());
    }

    @Test
    void resolvedMemoryEqualsTest() {
        Supplier<Character> s0 = () -> (char) 10;
        Supplier<Character> s1 = () -> (char) 20;
        Consumer<Character> c0 = c -> {};
        Consumer<Character> c1 = this::discard;
        assertTrue(equalsCoverageTest(
                new ResolvedMemory(s0, c0),
                new ResolvedMemory(s0, c0),
                new ResolvedMemory(s1, c0),
                _const((char) 0x100)
        ));
        assertTrue(equalsCoverageTest(
                new ResolvedMemory(s0, c0),
                new ResolvedMemory(s0, c0),
                new ResolvedMemory(s0, c1),
                _const((char) 0x100)
        ));
        assertTrue(equalsCoverageTest(
                new ResolvedMemory(s0, c0),
                new ResolvedMemory(s0, c0),
                new ResolvedMemory(s1, c1),
                _const((char) 0x100)
        ));
    }
}
