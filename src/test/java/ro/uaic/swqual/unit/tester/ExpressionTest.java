package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.tester.Expression;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            assertFalse(expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertTrue(expr.evaluate());
        });
    }

    @Test
    void parseNotEqualsShouldResolveToNotEquals() {
        expressionTest("r0 != 50", (expr, regs) -> {
            assertTrue(expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertFalse(expr.evaluate());
        });
    }

    @Test
    void parseLessThanShouldResolveToLessThan() {
        expressionTest("r0 < 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertTrue(expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertFalse(expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertFalse(expr.evaluate());
        });
    }

    @Test
    void parseLessEqualsShouldResolveToLessEquals() {
        expressionTest("r0 <= 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertTrue(expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertTrue(expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertFalse(expr.evaluate());
        });
    }

    @Test
    void parseGreaterThanShouldResolveToGreaterThan() {
        expressionTest("r0 > 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertFalse(expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertFalse(expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertTrue(expr.evaluate());
        });
    }

    @Test
    void parseGreaterEqualsShouldResolveToGreaterEquals() {
        expressionTest("r0 >= 50", (expr, regs) -> {
            regs.getFirst().setValue((char) 49);
            assertFalse(expr.evaluate());
            regs.getFirst().setValue((char) 50);
            assertTrue(expr.evaluate());
            regs.getFirst().setValue((char) 51);
            assertTrue(expr.evaluate());
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

        assertFalse(expr.evaluate());
        regs.getFirst().setValue((char) 50);
        assertTrue(expr.evaluate());
    }
}
