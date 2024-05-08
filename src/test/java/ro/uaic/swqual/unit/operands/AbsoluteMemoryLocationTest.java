package ro.uaic.swqual.unit.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Register;

class AbsoluteMemoryLocationTest {
    @Test
    void basicTest() {
        var register = new Register();
        var constant = new Constant((char) 10);
        register.setValue((char) 25);

        var loc1 = new AbsoluteMemoryLocation(register);
        var loc2 = new AbsoluteMemoryLocation(constant);

        Assertions.assertEquals((char) 25, loc1.getValue());
        Assertions.assertEquals((char) 10, loc2.getValue());

        register.setValue((char) 400);
        Assertions.assertEquals((char) 400, loc1.getValue());
    }
}
