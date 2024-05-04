package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.operands.Register;

public class RegisterTest {
    @Test
    public void dataRangeTest() {
        var register = new Register();
        try {
            register.setValue(1234);
            Assert.assertEquals(1234, register.getValue());
        } catch (ValueException e) {
            Assert.fail(e.getMessage());
        }

        try {
            register.setValue(1000000);
            Assert.fail("Unexpected value accepted");
        } catch (ValueException e) {
            // nothing, test successful
        }
    }
}
