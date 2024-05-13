package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterTest implements ProcTestUtility, RegisterTestUtility {
    @Test
    void dataRangeTest() {
        var register = new Register();
        try {
            register.setValue(0x1234);
            assertEquals(0x1234, register.getValue());

            register.setValue(0xFFFF);
            assertEquals(0xFFFF, register.getValue());
        } catch (ValueException e) {
            Assertions.fail(e.getMessage());
        }

        try {
            register.setValue(0xFFFF + 1);
            Assertions.fail("Unexpected value accepted");
        } catch (ValueException e) {
            // nothing, test successful
        }

        try {
            register.setValue(-1);
            Assertions.fail("Unexpected value accepted");
        } catch (ValueException e) {
            // nothing, test successful
        }
    }

    @Test
    void equalsTest() {
        assertTrue(equalsCoverageTest(
                reg((char) 0x1234),
                reg((char) 0x1234),
                reg((char) 0xDEAD),
                _const((char) 0x1234)
        ));
    }

    @Test
    void toStringShouldResolve() {
        assertEquals("reg(15)", reg((char) 15).toString());
    }
}
