package ro.uaic.swqual.unit.operands;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.RelativeMemoryLocation;

import java.util.List;

class RelativeMemoryLocationTest implements TestUtility {
    @Test
    void fromSingleOperand() {
        exceptionLess(() -> {
            var register = new Register();
            var constant = new Constant((char) 10);
            register.setValue((char) 25);

            var loc1 = new RelativeMemoryLocation(List.of(register), List.of());
            var loc2 = new RelativeMemoryLocation(List.of(constant), List.of());

            Assertions.assertEquals((char) 25, loc1.getValue());
            Assertions.assertEquals((char) 10, loc2.getValue());

            register.setValue((char) 400);
            Assertions.assertEquals((char) 400, loc1.getValue());
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
            Assertions.assertEquals((char) 34, loc.getValue());

            r2.setValue(10);
            Assertions.assertEquals((char) 14, loc.getValue());

            r1.setValue(50);
            Assertions.assertEquals((char) 54, loc.getValue());
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
}
