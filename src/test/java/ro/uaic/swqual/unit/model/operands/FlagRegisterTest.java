package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.model.operands.FlagRegister;

class FlagRegisterTest {
    @Test
    void clearTest() {
        var register = new FlagRegister();
        register.set((char) 0x00FF);
        Assertions.assertEquals((char) 0x00FF, register.getValue());
        register.clear();
        Assertions.assertEquals((char) 0x0000, register.getValue());
    }

    @Test
    void set() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        Assertions.assertEquals(FlagRegister.OVERFLOW_FLAG, register.getValue());
        register.set(FlagRegister.EQUAL_FLAG);
        Assertions.assertEquals(FlagRegister.OVERFLOW_FLAG | FlagRegister.EQUAL_FLAG, register.getValue());
    }

    @Test
    void unset() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        register.set(FlagRegister.ZERO_FLAG);
        register.set(FlagRegister.DIV_ZERO_FLAG);
        Assertions.assertEquals(
                FlagRegister.OVERFLOW_FLAG | FlagRegister.ZERO_FLAG | FlagRegister.DIV_ZERO_FLAG,
                register.getValue()
        );
        register.unset(FlagRegister.ZERO_FLAG);
        Assertions.assertEquals(FlagRegister.OVERFLOW_FLAG | FlagRegister.DIV_ZERO_FLAG, register.getValue());
    }

    @Test
    void isSet() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        register.set(FlagRegister.ZERO_FLAG);
        Assertions.assertTrue(register.isSet(FlagRegister.OVERFLOW_FLAG));
        Assertions.assertTrue(register.isSet(FlagRegister.ZERO_FLAG));
        Assertions.assertFalse(register.isSet(FlagRegister.DIV_ZERO_FLAG));
    }
}
