package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ParameterException;

public class MemoryLocationTest {
    MemoryLocation mockMemoryLocation(char givenValue) {
        return new MemoryLocation() {{
            value = givenValue;
        }};
    }

    @Test
    public void memoryLocationShouldNotBeImplicitlyWriteable() {
        var loc = mockMemoryLocation((char) 100);
        Assert.assertThrows(ParameterException.class, () -> loc.setValue((char) 200));
    }

    @Test
    public void memoryLocationShouldBeImplicitlyReadable() {
        var loc = mockMemoryLocation((char) 200);
        Assert.assertEquals(200, loc.getValue());
    }
}
