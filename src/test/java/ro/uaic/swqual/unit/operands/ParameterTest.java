package ro.uaic.swqual.unit.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.Parameter;

class ParameterTest {
    Parameter mockParameter(char givenValue) {
        return new Parameter() {{
            value = givenValue;
        }};
    }

    @Test
    void parameterShouldBeReadable() {
        var param = mockParameter((char) 10);
        Assertions.assertEquals(10, param.getValue());
    }

    @Test
    void parameterShouldNotBeWriteable() {
        var param = mockParameter((char) 40);
        Assertions.assertThrows(ParameterException.class, () -> param.setValue((char) 20));
    }
}
