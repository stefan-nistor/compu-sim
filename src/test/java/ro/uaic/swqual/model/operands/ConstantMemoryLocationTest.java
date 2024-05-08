package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ParameterException;

public class ConstantMemoryLocationTest {
    @Test
    public void directMemoryLocationShouldBeMemoryLocation() {
        var dml = new ConstantMemoryLocation((char) 0x100);
        Assert.assertTrue(dml instanceof MemoryLocation);
    }

    @Test
    public void directMemoryLocationShouldResolveToConstructedValue() {
        var value = (char) 0x50;
        var dml = new ConstantMemoryLocation(value);
        Assert.assertEquals(value, dml.getValue());
    }

    @Test
    public void directMemoryLocationShouldNotBeWriteable() {
        var dml = new ConstantMemoryLocation((char) 0x100);
        Assert.assertThrows(ParameterException.class, () -> dml.setValue((char) 0x50));
    }
}
