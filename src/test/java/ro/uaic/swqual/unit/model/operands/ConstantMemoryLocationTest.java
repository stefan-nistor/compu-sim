package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.MemoryLocation;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(value, dml.getValue());
    }

    @Test
    void directMemoryLocationShouldNotBeWriteable() {
        var dml = new ConstantMemoryLocation((char) 0x100);
        Assertions.assertThrows(ParameterException.class, () -> dml.setValue((char) 0x50));
    }

    @Test
    void resolveInnerRefsDoesNothing() {
        var cloc = new ConstantMemoryLocation((char) 0x100);
        cloc.resolveInnerReferences(Map.of());
        assertEquals((char) 0x100, cloc.getValue());
    }

    @Test
    void toStringShouldResolve() {
        var cloc = new ConstantMemoryLocation((char) 0x100);
        assertEquals("[0x100]", cloc.toString());
    }
}
