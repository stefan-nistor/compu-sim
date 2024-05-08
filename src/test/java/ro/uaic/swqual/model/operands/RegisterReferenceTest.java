package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.TestUtility;
import ro.uaic.swqual.exception.ParameterException;

public class RegisterReferenceTest implements TestUtility {
    @Test
    public void registerReferenceShouldTrackLineAndName() {
        var regRef = new RegisterReference(10, "r0");
        Assert.assertEquals(10, regRef.getReferencedAtLine());
        Assert.assertEquals("r0", regRef.getName());
    }

    @Test
    public void registerReferenceShouldThrowOnWrite() {
        var regRef = new RegisterReference(10, "r0");
        Assert.assertThrows(ParameterException.class, () -> regRef.setValue((char) 0));
    }

    @Test
    public void registerReferenceShouldThrowOnReadChar() {
        var regRef = new RegisterReference(10, "r0");
        Assert.assertThrows(ParameterException.class, () -> discard(regRef.getValue()));
    }
}
