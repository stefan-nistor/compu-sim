package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.CPU;
import ro.uaic.swqual.tester.Expectation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpectationTest {
    interface ExpectationConsumer {
        void accept(Expectation expectation, List<Register> regList);
    }

    void expectationTest(String expectation, ExpectationConsumer expectationConsumer) {
        var cpu = new CPU();
        var refMap = cpu.getRegistryReferenceMap();
        var regs = cpu.getDataRegisters();
        var exp = Expectation.from(expectation);
        if (exp != null) {
            exp.referencing(refMap);
        }
        expectationConsumer.accept(exp, regs);
    }

    @Test
    void trueExpectationShouldEvaluateExpectedly() {
        var cpu = new CPU();
        var regs = cpu.getDataRegisters();
        var map = cpu.getRegistryReferenceMap();

        var expectation = Expectation.from("expect-true {r0==50}");
        assertNotNull(expectation);
        regs.getFirst().setValue((char) 50);
        assertTrue(expectation.referencing(map).evaluate());
    }

    @Test
    void invalidExpectationShouldResolveToNullExpectation() {
        expectationTest("expect-unknown {r0 == 50}", (expectation, regList) -> assertNull(expectation));
        expectationTest("expect-something {r0 != r1}", (expectation, regList) -> assertNull(expectation));
    }
}
