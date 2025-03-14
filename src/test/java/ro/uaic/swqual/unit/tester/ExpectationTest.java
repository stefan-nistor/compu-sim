package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.tester.Expectation;
import ro.uaic.swqual.tester.ExpressionExpectation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpectationTest {
    interface ExpectationConsumer {
        void accept(Expectation expectation, List<Register> regList);
    }

    void expectationTest(String expectation, ExpectationConsumer expectationConsumer) {
        var cpu = new CentralProcessingUnit();
        var refMap = cpu.getRegistryReferenceMap();
        var regs = cpu.getDataRegisters();
        var exp = Expectation.from(expectation);
        if (exp != null) {
            assertInstanceOf(ExpressionExpectation.class, exp);
            ((ExpressionExpectation)exp).referencing(refMap);
        }
        expectationConsumer.accept(exp, regs);
    }

    @Test
    void trueExpectationShouldEvaluateExpectedly() {
        var cpu = new CentralProcessingUnit();
        var regs = cpu.getDataRegisters();
        var map = cpu.getRegistryReferenceMap();

        var expectation = Expectation.from("expect-true {r0==50}");
        assertNotNull(expectation);
        assertInstanceOf(ExpressionExpectation.class, expectation);
        var exprExp = (ExpressionExpectation) expectation;
        assertFalse(exprExp.referencing(map).evaluate());
        regs.getFirst().setValue((char) 50);
        assertTrue(exprExp.referencing(map).evaluate());
    }

    @Test
    void falseExpectationShouldEvaluateExpectedly() {
        var cpu = new CentralProcessingUnit();
        var regs = cpu.getDataRegisters();
        var map = cpu.getRegistryReferenceMap();

        var expectation = Expectation.from("expect-false {r0==50}");
        assertNotNull(expectation);
        assertInstanceOf(ExpressionExpectation.class, expectation);
        var exprExp = (ExpressionExpectation) expectation;
        assertTrue(exprExp.referencing(map).evaluate());
        regs.getFirst().setValue((char) 50);
        assertFalse(exprExp.referencing(map).evaluate());
    }

    @Test
    void invalidExpectationShouldResolveToNullExpectation() {
        expectationTest("expect-unknown {r0 == 50}", (expectation, regList) -> assertNull(expectation));
        expectationTest("expect-something {r0 != r1}", (expectation, regList) -> assertNull(expectation));
    }
}
