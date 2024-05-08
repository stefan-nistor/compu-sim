package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ParameterException;

public class ParameterTest {
    Parameter mockParameter(char givenValue) {
        return new Parameter() {{
            value = givenValue;
        }};
    }

    @Test
    public void parameterShouldBeReadable() {
        var param = mockParameter((char) 10);
        Assert.assertEquals(10, param.getValue());
    }

    @Test
    public void parameterShouldNotBeWriteable() {
        var param = mockParameter((char) 40);
        Assert.assertThrows(ParameterException.class, () -> param.setValue((char) 20));
    }
}
