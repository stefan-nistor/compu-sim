package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ro.uaic.swqual.model.operands.FlagRegister.EQUAL_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.ZERO_FLAG;

class FlagRegisterTest implements ProcTestUtility {
    @Test
    void clearTest() {
        var register = new FlagRegister();
        register.set((char) 0x00FF);
        assertEquals((char) 0x00FF, register.getValue());
        register.clear();
        assertEquals((char) 0x0000, register.getValue());
    }

    @Test
    void set() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        assertEquals(FlagRegister.OVERFLOW_FLAG, register.getValue());
        register.set(FlagRegister.EQUAL_FLAG);
        assertEquals(FlagRegister.OVERFLOW_FLAG | FlagRegister.EQUAL_FLAG, register.getValue());
    }

    @Test
    void unset() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        register.set(ZERO_FLAG);
        register.set(FlagRegister.DIV_ZERO_FLAG);
        assertEquals(
                FlagRegister.OVERFLOW_FLAG | ZERO_FLAG | FlagRegister.DIV_ZERO_FLAG,
                register.getValue()
        );
        register.unset(ZERO_FLAG);
        assertEquals(FlagRegister.OVERFLOW_FLAG | FlagRegister.DIV_ZERO_FLAG, register.getValue());
    }

    @Test
    void isSet() {
        var register = new FlagRegister();
        register.set(FlagRegister.OVERFLOW_FLAG);
        register.set(ZERO_FLAG);
        Assertions.assertTrue(register.isSet(FlagRegister.OVERFLOW_FLAG));
        Assertions.assertTrue(register.isSet(ZERO_FLAG));
        Assertions.assertFalse(register.isSet(FlagRegister.DIV_ZERO_FLAG));
    }

    @Test
    void toStringShouldResolveToStringWithCommaSeparatedFlags() {
        var flags = freg();
        flags.set(ZERO_FLAG);
        flags.set(EQUAL_FLAG);
        flags.set(SEG_FLAG);
        assertEquals("ZERO, EQ, SEG", flags.toString());
    }
}
