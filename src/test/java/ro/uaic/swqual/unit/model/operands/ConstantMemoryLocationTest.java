package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.MemoryLocation;

class ConstantMemoryLocationTest {
    @Test
    void directMemoryLocationShouldBeMemoryLocation() {
        var dml = new ConstantMemoryLocation((char) 0x100);
        Assertions.assertInstanceOf(MemoryLocation.class, dml);
    }

    @Test
    void directMemoryLocationShouldResolveToConstructedValue() {
        var value = (char) 0x50;
        var dml = new ConstantMemoryLocation(value);
        Assertions.assertEquals(value, dml.getValue());
    }

    @Test
    void directMemoryLocationShouldNotBeWriteable() {
        var dml = new ConstantMemoryLocation((char) 0x100);
        Assertions.assertThrows(ParameterException.class, () -> dml.setValue((char) 0x50));
    }
}
