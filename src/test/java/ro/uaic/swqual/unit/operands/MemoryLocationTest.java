package ro.uaic.swqual.unit.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.MemoryLocation;

class MemoryLocationTest {
    MemoryLocation mockMemoryLocation(char givenValue) {
        return new MemoryLocation() {{
            value = givenValue;
        }};
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
