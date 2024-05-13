package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.RegisterReference;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterReferenceTest implements RegisterTestUtility, ProcTestUtility {
    @Test
    void registerReferenceShouldTrackLineAndName() {
        var regRef = new RegisterReference(10, "r0");
        assertEquals(10, regRef.getReferencedAtLine());
        assertEquals("r0", regRef.getName());
    }

    @Test
    void registerReferenceShouldThrowOnWrite() {
        var regRef = new RegisterReference(10, "r0");
        Assertions.assertThrows(ParameterException.class, () -> regRef.setValue((char) 0));
    }

    @Test
    void registerReferenceShouldThrowOnReadChar() {
        var regRef = new RegisterReference(10, "r0");
        Assertions.assertThrows(ParameterException.class, () -> discard(regRef.getValue()));
    }

    @Test
    void regRefEqualsTest() {
        assertTrue(equalsCoverageTest(
                new RegisterReference(0, "test"),
                new RegisterReference(0, "test"),
                new RegisterReference(0, "diff"),
                new Constant((char) 0xABCD)
        ));

        assertTrue(equalsCoverageTest(
                new RegisterReference(0, "test"),
                new RegisterReference(0, "test"),
                new RegisterReference(1, "test"),
                new Constant((char) 0xABCD)
        ));

        assertTrue(equalsCoverageTest(
                new RegisterReference(0, "test"),
                new RegisterReference(0, "test"),
                new RegisterReference(1, "diff"),
                new Constant((char) 0xABCD)
        ));
    }

    @Test
    void toStringShouldResolve() {
        var ref = ref("r0");
        assertEquals("ref(r0)", ref.toString());
    }
}
