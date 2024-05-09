package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.Constant;

class ConstantTest {
    @Test
    void constantShouldNotBeReadable() {
        var constant = new Constant((char) 0x100);
        Assertions.assertThrows(ParameterException.class, () -> constant.setValue((char) 0x50));
    }

    @Test
    void constantValueShouldResolveToConstructedValue() {
        var value = (char) 0x50;
        var constant = new Constant(value);
        Assertions.assertEquals(value, constant.getValue());
    }
}
