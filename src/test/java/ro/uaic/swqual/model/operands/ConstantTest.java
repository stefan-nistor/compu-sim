package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ParameterException;

public class ConstantTest {
    @Test
    public void constantShouldNotBeReadable() {
        var constant = new Constant((char) 0x100);
        Assert.assertThrows(ParameterException.class, () -> constant.setValue((char) 0x50));
    }

    @Test
    public void constantValueShouldResolveToConstructedValue() {
        var value = (char) 0x50;
        var constant = new Constant(value);
        Assert.assertEquals(value, constant.getValue());
    }
}
