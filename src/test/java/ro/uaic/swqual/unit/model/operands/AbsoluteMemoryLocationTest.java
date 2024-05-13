package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.unit.mem.MemTestUtility;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void partialResolveReferencesShouldResolveInTheEnd() {
        var r0 = reg();
        var loc = aloc(ref("r0"));
        var m0 = Map.of("r1", reg());
        var m1 = Map.of("r0", r0);
        assertThrows(ParameterException.class, () -> discard(loc.getValue()));
        loc.resolveInnerReferences(m0);
        assertThrows(ParameterException.class, () -> discard(loc.getValue()));
        loc.resolveInnerReferences(m1);
        r0.setValue((char) 16);
        assertEquals(16, loc.getValue());
    }

    @Test
    void toStringShouldResolve() {
        var r0 = reg();
        var loc = aloc(r0);
        r0.setValue((char) 10);
        assertEquals("[reg(10)] (=0xa)", loc.toString());
    }
}
