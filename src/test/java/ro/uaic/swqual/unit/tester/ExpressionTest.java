package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.mem.RandomAccessMemory;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.tester.Expression;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.uaic.swqual.tester.Expression.EvaluationType.FALSE;
import static ro.uaic.swqual.tester.Expression.EvaluationType.TRUE;
import static ro.uaic.swqual.tester.Expression.EvaluationType.UNKNOWN;

class ExpressionTest {
    interface ExpressionConsumer {
        void accept(Expression expression, List<Register> registers);
    }

    void expressionTest(String exprStr, ExpressionConsumer consumer) {
        var cpu = new CentralProcessingUnit();
        var regs = cpu.getDataRegisters();
        var refMap = cpu.getRegistryReferenceMap();
        var expr = Expression.from(exprStr);
        if (expr != null) {
            expr.resolveReferences(refMap);
        }

        consumer.accept(expr, regs);
    }

    @Test
    void parseEqualsShouldResolveToEquals() {
        expressionTest("r0 == 50", (expr, regs) -> {
            assertEquals(FALSE, expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertEquals(TRUE, expr.evaluate());
        });
    }

    @Test
    void parseNotEqualsShouldResolveToNotEquals() {
        expressionTest("r0 != 50", (expr, regs) -> {
            assertEquals(TRUE, expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertEquals(FALSE, expr.evaluate());
        });
    }

    @Test
    void parseLessThanShouldResolveToLessThan() {
        expressionTest("r0 < 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertEquals(TRUE, expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertEquals(FALSE, expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertEquals(FALSE, expr.evaluate());
        });
    }

    @Test
    void parseLessEqualsShouldResolveToLessEquals() {
        expressionTest("r0 <= 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertEquals(TRUE, expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertEquals(TRUE, expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertEquals(FALSE, expr.evaluate());
        });
    }

    @Test
    void parseGreaterThanShouldResolveToGreaterThan() {
        expressionTest("r0 > 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertEquals(FALSE, expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertEquals(FALSE, expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertEquals(TRUE, expr.evaluate());
        });
    }

    @Test
    void parseGreaterEqualsShouldResolveToGreaterEquals() {
        expressionTest("r0 >= 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertEquals(FALSE, expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertEquals(TRUE, expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertEquals(TRUE, expr.evaluate());
        });
    }

    @Test
    void parseInvalidExpressionShouldResultInNullExpression() {
        expressionTest("", (expr, regs) -> assertNull(expr));
    }

    @Test
    void parseInvalidExpressionShouldThrow() {
        assertThrows(ParameterException.class, () -> expressionTest("r0 === r1", (expr, regs) -> {}));
        assertThrows(ParameterException.class, () -> expressionTest("r0 == r1+", (expr, regs) -> {}));
    }

    @Test
    void partialResolveDoesNotOverrideOldResolves() {
        var cpu = new CentralProcessingUnit();
        var regs = cpu.getDataRegisters();
        var expr = Expression.from("r0==50");
        assertNotNull(expr);
        expr.resolveReferences(Map.of());
        expr.resolveReferences(Map.of("r1", regs.get(1)));
        expr.resolveReferences(Map.of("r0", regs.get(0)));

        assertEquals(FALSE, expr.evaluate());
        regs.getFirst().setValue((char) 50);
        assertEquals(TRUE, expr.evaluate());
    }

    @Test
    void evaluationOfUnknownContextShouldEvaluateToFalse() {
        var expr1 = Expression.from("[0x100] == 50");
        assertNotNull(expr1);
        assertEquals(UNKNOWN, expr1.evaluate());
        var expr2 = Expression.from("50 == [0x100]");
        assertNotNull(expr2);
        assertEquals(UNKNOWN, expr2.evaluate());
    }

    @Test
    void evaluationOfKnownContextShouldEvaluateCorrectly() {
        var expr = Expression.from("[0x100] == 50");
        var ram = new RandomAccessMemory((char) 0x1000, new FlagRegister());
        assertNotNull(expr);
        expr.readAddressesFrom(ram, (char) 0x0, (char) 0x1000);
        assertEquals(FALSE, expr.evaluate());
        ram.write(new ConstantMemoryLocation((char) 0x100), (char) 50);
        assertEquals(TRUE, expr.evaluate());
    }
}
