package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;

public class FlagRegisterTest {
    @Test
    public void clearTest() {
        var register = new FlagRegister();
        register.set((char) 0x00FF);
        Assert.assertEquals((char) 0x00FF, register.getValue());
        register.clear();
        Assert.assertEquals((char) 0x0000, register.getValue());
    }

    @Test
    public void set() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        Assert.assertEquals(FlagRegister.OVERFLOW_FLAG, register.getValue());
        register.set(FlagRegister.EQUAL_FLAG);
        Assert.assertEquals(FlagRegister.OVERFLOW_FLAG | FlagRegister.EQUAL_FLAG, register.getValue());
    }

    @Test
    public void unset() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        register.set(FlagRegister.ZERO_FLAG);
        register.set(FlagRegister.DIV_ZERO_FLAG);
        Assert.assertEquals(
                FlagRegister.OVERFLOW_FLAG | FlagRegister.ZERO_FLAG | FlagRegister.DIV_ZERO_FLAG,
                register.getValue()
        );
        register.unset(FlagRegister.ZERO_FLAG);
        Assert.assertEquals(FlagRegister.OVERFLOW_FLAG | FlagRegister.DIV_ZERO_FLAG, register.getValue());
    }

    @Test
    public void isSet() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        register.set(FlagRegister.ZERO_FLAG);
        Assert.assertTrue(register.isSet(FlagRegister.OVERFLOW_FLAG));
        Assert.assertTrue(register.isSet(FlagRegister.ZERO_FLAG));
        Assert.assertFalse(register.isSet(FlagRegister.DIV_ZERO_FLAG));
    }
}
