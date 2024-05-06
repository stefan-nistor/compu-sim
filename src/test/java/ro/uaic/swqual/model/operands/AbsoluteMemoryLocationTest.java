package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;

public class AbsoluteMemoryLocationTest {
    @Test
    public void basicTest() {
        var register = new Register();
        var constant = new Constant((char) 10);
        register.setValue((char) 25);

        var loc1 = new AbsoluteMemoryLocation(register);
        var loc2 = new AbsoluteMemoryLocation(constant);

        Assert.assertEquals((char) 25, loc1.getValue());
        Assert.assertEquals((char) 10, loc2.getValue());

        register.setValue((char) 400);
        Assert.assertEquals((char) 400, loc1.getValue());
    }
}
