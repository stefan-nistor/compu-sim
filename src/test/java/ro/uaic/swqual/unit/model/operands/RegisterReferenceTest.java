package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.RegisterReference;

class RegisterReferenceTest implements TestUtility {
    @Test
    void registerReferenceShouldTrackLineAndName() {
        var regRef = new RegisterReference(10, "r0");
        Assertions.assertEquals(10, regRef.getReferencedAtLine());
        Assertions.assertEquals("r0", regRef.getName());
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
}
