package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ValueException;

public class ProcessorTest {
    @Test
    public void processorDataRegSize() {
        var processor = new Processor();
        var dataRegs = processor.getDataRegisters();
        Assert.assertEquals(dataRegs.size(), 8);
    }

    @Test
    public void processorDataRegStore() {
        try {
            var processor = new Processor();
            var dataRegs = processor.getDataRegisters();
            var reg1 = dataRegs.get(3);
            var reg2 = dataRegs.get(5);
            reg1.setValue(1234);
            reg2.setValue(5678);
            Assert.assertEquals(reg1.getValue(), 1234);
            Assert.assertEquals(reg2.getValue(), 5678);
            Assert.assertEquals(processor.getDataRegisters().get(3).getValue(), 1234);
            Assert.assertEquals(processor.getDataRegisters().get(5).getValue(), 5678);
        } catch (ValueException exception) {
            Assert.fail(exception.getMessage());
        }
    }
}
