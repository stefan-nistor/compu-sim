package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.TestUtility;
import ro.uaic.swqual.exception.ValueException;

import java.util.List;

public class RelativeMemoryLocationTest implements TestUtility {
    @Test
    public void fromSingleOperand() {
        exceptionLess(() -> {
            var register = new Register();
            var constant = new Constant((char) 10);
            register.setValue((char) 25);

            var loc1 = new RelativeMemoryLocation(List.of(register), List.of());
            var loc2 = new RelativeMemoryLocation(List.of(constant), List.of());

            Assert.assertEquals((char) 25, loc1.getValue());
            Assert.assertEquals((char) 10, loc2.getValue());

            register.setValue((char) 400);
            Assert.assertEquals((char) 400, loc1.getValue());
        });
    }

    @Test
    public void fromMultipleOperands() {
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
            Assert.assertEquals((char) 34, loc.getValue());

            r2.setValue(10);
            Assert.assertEquals((char) 14, loc.getValue());

            r1.setValue(50);
            Assert.assertEquals((char) 54, loc.getValue());
        });
    }

    @Test
    public void illFormedRelativeLocationTest() {
        // [a b] is ill-formed, lacking relation
        Assert.assertThrows(ValueException.class, () -> {
            var r1 = new Register();
            var r2 = new Register();
            new RelativeMemoryLocation(List.of(r1, r2), List.of());
        });

        // [a + b +] is ill-formed, lacking parameter
        Assert.assertThrows(ValueException.class, () -> {
            var r1 = new Register();
            var r2 = new Register();
            new RelativeMemoryLocation(List.of(r1, r2), List.of((a, b) -> (char)(a + b), (a, b) -> (char)(a + b)));
        });
    }
}
