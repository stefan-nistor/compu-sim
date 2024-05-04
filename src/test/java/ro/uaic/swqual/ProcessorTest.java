package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ValueException;

public class ProcessorTest {
    @Test
    public void processorDataRegSize() {
        var processor = new Processor();
        var dataRegs = processor.getDataRegisters();
        Assert.assertEquals(8, dataRegs.size());
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
            Assert.assertEquals(1234, reg1.getValue());
            Assert.assertEquals(5678, reg2.getValue());
            Assert.assertEquals(1234, processor.getDataRegisters().get(3).getValue());
            Assert.assertEquals(5678, processor.getDataRegisters().get(5).getValue());
        } catch (ValueException exception) {
            Assert.fail(exception.getMessage());
        }
    }
}
