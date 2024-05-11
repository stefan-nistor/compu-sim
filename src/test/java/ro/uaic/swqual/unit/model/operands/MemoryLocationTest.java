package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.Register;

import java.util.Map;

class MemoryLocationTest {
    MemoryLocation mockMemoryLocation(char givenValue) {
        return new MemoryLocation() {
            @Override
            public void resolveInnerReferences(Map<String, Register> registerMap) {
                // do nothing
            }

            {
                value = givenValue;
            }
        };
    }

    @Test
    void memoryLocationShouldNotBeImplicitlyWriteable() {
        var loc = mockMemoryLocation((char) 100);
        Assertions.assertThrows(ParameterException.class, () -> loc.setValue((char) 200));
    }

    @Test
    void memoryLocationShouldBeImplicitlyReadable() {
        var loc = mockMemoryLocation((char) 200);
        Assertions.assertEquals(200, loc.getValue());
    }
}
