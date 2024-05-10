package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.unit.mem.MemTestUtility;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbsoluteMemoryLocationTest implements ProcTestUtility, MemTestUtility, RegisterTestUtility {
    @Test
    void basicTest() {
        var register = new Register();
        var constant = new Constant((char) 10);
        register.setValue((char) 25);

        var loc1 = new AbsoluteMemoryLocation(register);
        var loc2 = new AbsoluteMemoryLocation(constant);

        assertEquals((char) 25, loc1.getValue());
        assertEquals((char) 10, loc2.getValue());

        register.setValue((char) 400);
        assertEquals((char) 400, loc1.getValue());
    }

    @Test
    void equalsTest() {
        assertTrue(equalsCoverageTest(
                aloc(reg((char) 0x1234)),
                aloc(_const((char) 0x1234)),
                aloc(reg((char) 0xDEAD)),
                _const((char) 0x1234)
        ));
    }

    @Test
    void hashCodeTest() {
        assertEquals(aloc(reg((char) 0xBEEF)).hashCode(), aloc(_const((char) 0xBEEF)).hashCode());
    }
}
