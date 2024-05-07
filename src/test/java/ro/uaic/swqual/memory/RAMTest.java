package ro.uaic.swqual.memory;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.mem.RAM;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.proc.ProcTestUtility;

public class RAMTest implements MemTestUtility, ProcTestUtility {
    @Test
    public void readWriteCompleteTest() {
        exceptionLess(() -> {
            var flags = reg();
            var ram = new RAM(65536, flags);
            var value = 0;
            var addr = reg();
            var loc = aloc(addr);
            for (int i = 0; i < 65536; i+=2) {
                ram.write(loc, (char) value);
                value += 4;
                addr.setValue((char) (addr.getValue() + 2));
            }

            addr.setValue((char) 0);
            var cmpValue = 0;
            for (int i = 0; i < 65536; i+=2) {
                var atMem = ram.read(loc);
                Assert.assertEquals(atMem, (char)cmpValue);
                cmpValue += 4;
                addr.setValue((char) (addr.getValue() + 2));
            }
        });
    }

    @Test
    public void writeEvenReadOddTest() {
        exceptionLess(() -> {
            // Little endian, keep in mind.

            var flags = reg();
            var ram = new RAM(1024, flags);
            var addr = reg();
            var loc = aloc(addr);

            addr.setValue(0x100);
            ram.write(loc, (char) 0x5678);
            addr.setValue(0x102);
            ram.write(loc, (char) 0x1234);
            // LE Memory layout:
            // 0x100 0x101 0x102 0x103
            // 0x78  0x56  0x34  0x12

            addr.setValue(0x101);
            var read = ram.read(loc);
            Assert.assertEquals((char) 0x3456, read);
        });
    }

    @Test
    public void partialOverwriteTest() {
        exceptionLess(() -> {
            var flags = reg();
            var ram = new RAM(1024, flags);
            var addr = reg();
            var loc = aloc(addr);

            addr.setValue(0x100);
            ram.write(loc, (char) 0x1111);
            addr.setValue(0x102);
            ram.write(loc, (char) 0x2222);
            addr.setValue(0x101);
            ram.write(loc, (char) 0xFFFF);

            addr.setValue(0x100);
            var read1 = ram.read(loc);
            addr.setValue(0x102);
            var read2 = ram.read(loc);

            Assert.assertEquals((char) 0xFF11, read1);
            Assert.assertEquals((char) 0x22FF, read2);
        });
    }

    @Test
    public void segmentationTest() {
        exceptionLess(() -> {
            var flags = freg();
            var ram = new RAM(1024, flags);
            var addr = reg();
            var loc = aloc(addr);

            addr.setValue(1022);
            ram.write(loc, (char) 0x1234);
            Assert.assertEquals(0x1234, ram.read(loc));
            Assert.assertEquals((char) 0x0, flags.getValue());

            addr.setValue(1023);
            ram.write(loc, (char) 0x5678);
            Assert.assertTrue(flags.isSet(FlagRegister.SEG_FLAG));
            flags.clear();
            Assert.assertEquals(0, ram.read(loc));
            Assert.assertTrue(flags.isSet(FlagRegister.SEG_FLAG));
        });
    }

    @Test
    public void memorySizeTooLargeCreateTest() {
        Assert.assertThrows(ValueException.class, () -> new RAM(65537, freg()));
        Assert.assertThrows(ValueException.class, () -> new RAM(1, freg()));
    }
}
