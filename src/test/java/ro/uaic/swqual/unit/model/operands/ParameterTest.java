package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Parameter;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterTest implements RegisterTestUtility {
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

    @Test
    void equalsTest() {
        assertTrue(equalsCoverageTest(
                mockParameter((char) 0x1234),
                mockParameter((char) 0x1234),
                mockParameter((char) 0xDEAD),
                new Constant((char) 0x1234)
        ));
    }
}
