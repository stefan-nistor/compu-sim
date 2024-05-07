package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.mem.RAM;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.FlagRegister;

public class MMUTest implements ProcTestUtility {
    @Test
    public void movRegRegTest() {
        var r0 = reg(10);
        var r1 = reg();
        var sp = reg();
        var mmu = new MMU(freg(), sp);
        mmu.execute(new Instruction(InstructionType.MMU_MOV, r0, r1));
        Assert.assertEquals(r0.getValue(), r1.getValue());
    }

    @Test
    public void movRegRamTest() {
        exceptionLess(() -> {
            var sp = reg();
            var freg = freg();

            var mmu = new MMU(freg, sp);
            var ram = new RAM((char) 0x1000, freg);

            mmu.registerMemoryUnit(ram, (location) -> location.getValue() >= 0x100 && location.getValue() <= 0x1000);

            var addr = reg(0x50); // mov r0 0x50
            var loc = new AbsoluteMemoryLocation(addr); // define [r0] (add pointer)

            var location = mmu.locate(loc); // locate [r0]
            Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
            Assert.assertNotNull(location.getValue()); // actually access [r0]
            Assert.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));


            addr.setValue(0x150);
            location = mmu.locate(loc);
            freg.clear();

            Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
            Assert.assertNotNull(location.getValue());
            Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        });
    }
}
