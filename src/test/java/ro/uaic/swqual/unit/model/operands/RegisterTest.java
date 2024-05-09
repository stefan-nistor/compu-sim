package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

import java.util.Objects;

class RegisterTest {
    @Test
    void dataRangeTest() {
        var register = new Register();
        try {
            register.setValue(0x1234);
            Assertions.assertEquals(0x1234, register.getValue());

            register.setValue(0xFFFF);
            Assertions.assertEquals(0xFFFF, register.getValue());
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
    void hashCodeTest() {
        var register = new Register();
        register.setValue((char) 1234);
        Assertions.assertEquals(Objects.hash(register.getValue()), register.hashCode());
    }

    @Test
    void equalsTest() {
        var register = new Register();
        var otherRegister = new Register();
        var flagRegister = new FlagRegister();
        var constant = new Constant((char) 0x1234);

        register.setValue((char) 0x1234);
        otherRegister.setValue((char) 0x1234);
        Assertions.assertEquals(register, otherRegister);
        otherRegister.setValue((char) 0xDEAD);
        Assertions.assertNotEquals(register, otherRegister);

        flagRegister.set(FlagRegister.OVERFLOW_FLAG);
        Assertions.assertNotEquals(flagRegister, register);
        register.setValue(flagRegister.getValue());
        Assertions.assertNotEquals(flagRegister, register);

        register.setValue((char) 0x1234);
        Assertions.assertNotEquals(register, constant);

        Assertions.assertNotEquals(null, register);
        Assertions.assertNotEquals(register, null);
        Assertions.assertEquals(register, register);
    }
}
