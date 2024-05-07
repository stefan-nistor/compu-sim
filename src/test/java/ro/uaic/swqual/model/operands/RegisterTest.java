package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ValueException;

import java.util.Objects;

public class RegisterTest {
    @Test
    public void dataRangeTest() {
        var register = new Register();
        try {
            register.setValue(0x1234);
            Assert.assertEquals(0x1234, register.getValue());

            register.setValue(0xFFFF);
            Assert.assertEquals(0xFFFF, register.getValue());
        } catch (ValueException e) {
            Assert.fail(e.getMessage());
        }

        try {
            register.setValue(0xFFFF + 1);
            Assert.fail("Unexpected value accepted");
        } catch (ValueException e) {
            // nothing, test successful
        }

        try {
            register.setValue(-1);
            Assert.fail("Unexpected value accepted");
        } catch (ValueException e) {
            // nothing, test successful
        }
    }

    @Test
    public void hashCodeTest() {
        var register = new Register();
        register.setValue((char) 1234);
        Assert.assertEquals(Objects.hash(register.getValue()), register.hashCode());
    }

    @Test
    public void equalsTest() {
        var register = new Register();
        var otherRegister = new Register();
        var flagRegister = new FlagRegister();
        var constant = new Constant((char) 0x1234);

        register.setValue((char) 0x1234);
        otherRegister.setValue((char) 0x1234);
        Assert.assertEquals(register, otherRegister);
        otherRegister.setValue((char) 0xDEAD);
        Assert.assertNotEquals(register, otherRegister);

        flagRegister.set(FlagRegister.OVERFLOW_FLAG);
        Assert.assertNotEquals(flagRegister, register);
        register.setValue(flagRegister.getValue());
        Assert.assertNotEquals(flagRegister, register);

        register.setValue((char) 0x1234);
        Assert.assertNotEquals(register, constant);

        Assert.assertNotEquals(null, register);
        Assert.assertNotEquals(register, null);
        Assert.assertEquals(register, register);
    }
}
