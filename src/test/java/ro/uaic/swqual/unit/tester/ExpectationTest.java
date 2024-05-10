package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.proc.CPU;
import ro.uaic.swqual.tester.Expectation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpectationTest {
    @Test
    void trueExpectationShouldEvaluateExpectedly() {
        var cpu = new CPU();
        var regs = cpu.getDataRegisters();
        var map = cpu.registryReferenceMap;

        var expectation = Expectation.from("expect-true {r0==50}");
        assertNotNull(expectation);
        regs.getFirst().setValue((char) 50);
        assertTrue(expectation.referencing(map).evaluate());
    }
}
