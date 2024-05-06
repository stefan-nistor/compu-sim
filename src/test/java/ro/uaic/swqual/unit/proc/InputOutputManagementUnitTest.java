package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.peripheral.Keyboard;
import ro.uaic.swqual.proc.InputOutputManagementUnit;
import ro.uaic.swqual.unit.mem.MemTestUtility;

class InputOutputManagementUnitTest implements ProcTestUtility, MemTestUtility {

    private FlagRegister freg;
    private InputOutputManagementUnit io;
    private Keyboard kb;
    private static final char KB_ADDR = 0x0;

    @BeforeEach
    void setUp() {
        freg = freg();
        io = new InputOutputManagementUnit(freg);
        kb = new Keyboard();
        io.registerHardwareUnit(kb, (char)0, location -> location == KB_ADDR);
    }


    @Test
    void registerKbShouldReturnPressedKey() {
        var addr = reg(KB_ADDR);
        var loc = aloc(addr);
        var keyValue = io.locate(loc);

        kb.press('c');
        Assertions.assertEquals('c',keyValue.getValue() );
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    void registerKbWithBadAddrShouldSetSEG() {
        var addr = reg(KB_ADDR + 0x1);
        var loc = aloc(addr);
        var keyValue = io.locate(loc);
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        var value = keyValue.getValue();
        Assertions.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    void kbWithMultiplePresses() {
        var addr = reg(KB_ADDR);
        var loc = aloc(addr);
        var keyValue = io.locate(loc);
        kb.press((char) 112);
        kb.press((char) 117);
        kb.press((char) 108);
        kb.press((char) 97);

        Assertions.assertEquals(112, keyValue.getValue());
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));

        kb.onTick();
        Assertions.assertEquals(117, keyValue.getValue());
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));

        kb.onTick();
        Assertions.assertEquals(108, keyValue.getValue());
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));

        kb.onTick();
        Assertions.assertEquals(97, keyValue.getValue());
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    void kbReadWithNoKeyPressed() {
        var addr = reg(KB_ADDR);
        var loc = aloc(addr);
        var keyValue = io.locate(loc);
        Assertions.assertEquals(0x0, keyValue.getValue());
    }

    @Test
    void kbMultipleReadsWithNoKeyPressed() {
        var addr = reg(KB_ADDR);
        var loc = aloc(addr);
        var keyValue = io.locate(loc);

        kb.onTick();
        kb.onTick();
        Assertions.assertEquals(0x0, keyValue.getValue());
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
    }

}
