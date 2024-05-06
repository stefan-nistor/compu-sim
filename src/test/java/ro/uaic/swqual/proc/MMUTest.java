package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;

public class MMUTest implements ProcTestUtility {
    @Test
    public void movRegRegTest() {
        var r0 = reg(10);
        var r1 = reg();
        var sp = reg();
        var mmu = new MMU(null, sp);
        mmu.execute(new Instruction(InstructionType.MMU_MOV, r0, r1));
        Assert.assertEquals(r0.getValue(), r1.getValue());
    }
}
