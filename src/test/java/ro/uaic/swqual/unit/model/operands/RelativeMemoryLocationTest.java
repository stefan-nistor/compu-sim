package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.RegisterReference;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.RelativeMemoryLocation;
import ro.uaic.swqual.unit.mem.MemTestUtility;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelativeMemoryLocationTest implements TestUtility, RegisterTestUtility, MemTestUtility, ProcTestUtility {
    @Test
    void fromSingleOperand() {
        exceptionLess(() -> {
            var register = new Register();
            var constant = new Constant((char) 10);
            register.setValue((char) 25);

            var loc1 = new RelativeMemoryLocation(List.of(register), List.of());
            var loc2 = new RelativeMemoryLocation(List.of(constant), List.of());

            assertEquals((char) 25, loc1.getValue());
            assertEquals((char) 10, loc2.getValue());

            register.setValue((char) 400);
            assertEquals((char) 400, loc1.getValue());
        });
    }

    @Test
    void fromMultipleOperands() {
        exceptionLess(() -> {
            var r1 = new Register();
            var r2 = new Register();
            var c1 = new Constant((char) 6);

            // [r1 + r2 - 6]
            var loc = new RelativeMemoryLocation(
                    List.of(r1, r2, c1),
                    List.of(
                            (a, b) -> (char)(a + b),
                            (a, b) -> (char)(a - b)
                    )
            );

            r1.setValue(10);
            r2.setValue(30);
            assertEquals((char) 34, loc.getValue());

            r2.setValue(10);
            assertEquals((char) 14, loc.getValue());

            r1.setValue(50);
            assertEquals((char) 54, loc.getValue());
        });
    }

    @Test
    void illFormedRelativeLocationTest() {
        // [a b] is ill-formed, lacking relation
        Assertions.assertThrows(ValueException.class, () -> {
            var r1 = new Register();
            var r2 = new Register();
            new RelativeMemoryLocation(List.of(r1, r2), List.of());
        });

        // [a + b +] is ill-formed, lacking parameter
        Assertions.assertThrows(ValueException.class, () -> {
            var r1 = new Register();
            var r2 = new Register();
            new RelativeMemoryLocation(List.of(r1, r2), List.of((a, b) -> (char)(a + b), (a, b) -> (char)(a + b)));
        });
    }

    @Test
    void relativeLocationEqualsTest() {
        BinaryOperator<Character> r0 = (a, b) -> (char) (a - b);
        BinaryOperator<Character> r1 = (a, b) -> (char) (a + b);
        assertTrue(equalsCoverageTest(
                rloc(reg((char) 0xDEAD), r0, reg((char) 0xBEEF)),
                rloc(reg((char) 0xDEAD), r0, reg((char) 0xBEEF)),
                rloc(reg((char) 0xDEAD), r1, reg((char) 0xBEEF)),
                _const((char) 0xABCD)
        ));

        assertTrue(equalsCoverageTest(
                rloc(reg((char) 0xDEAD), r0, reg((char) 0xBEEF)),
                rloc(reg((char) 0xDEAD), r0, reg((char) 0xBEEF)),
                rloc(reg((char) 0xBEEF), r0, reg((char) 0xDEAD)),
                _const((char) 0xABCD)
        ));
    }

    @Test
    void relMemLocPartialResolveShouldSucceed() {
        var r0 = new Register();
        var r1 = new Register();
        var ref0 = new RegisterReference(0, "r0");
        var ref1 = new RegisterReference(1, "r1");
        var refMap0 = Map.of("r0", r0);
        var refMap1 = Map.of("r1", r1);
        var rloc = rloc(List.of(ref0, ref1), List.of((a, b) -> (char) (a + b)));

        assertThrows(ParameterException.class, () -> discard(rloc.getValue()));
        rloc.resolveInnerReferences(refMap0);
        assertThrows(ParameterException.class, () -> discard(rloc.getValue()));
        rloc.resolveInnerReferences(refMap1);
        assertEquals((char) 0, rloc.getValue());
    }

    @Test
    void toStringShouldResolveToUnknown() {
        var rloc = rloc(reg(0x20));
        assertEquals("[<relative-location>] (=0x20)", rloc.toString());
    }
}
