package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;

import java.util.Arrays;

public class CPUTest extends ProcTest {
    @Test
    public void processorDataRegSize() {
        var processor = new CPU();
        var dataRegs = processor.getDataRegisters();
        Assert.assertEquals(8, dataRegs.size());
    }

    @Test
    public void processorDataRegStore() {
        try {
            var processor = new CPU();
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

    @Test
    public void handleAllInstructions() {
        exceptionLess(() -> {
            var processor = new CPU();
            var dataRegs = processor.getDataRegisters();
            var reg0 = dataRegs.get(0);
            var reg1 = dataRegs.get(1);
            Arrays.stream(InstructionType.values())
                    .forEach(i -> exceptionLess(() -> processor.execute(new Instruction(i, reg0, reg1))));
        });
    }

    @Test
    public void handleInstructionPassing() {
        exceptionLess(() -> {
            var processor = new CPU();
            var dregs = processor.getDataRegisters();
            var freg = processor.getFlagRegister();
            var alu0 = new ALU(freg, dregs.get(7));
            processor.registerUnit(alu0);
            dregs.get(0).setValue((char)0x1002);
            dregs.get(1).setValue((char)0x5000);
            processor.execute(new Instruction(InstructionType.ALU_UMUL, dregs.get(0), dregs.get(1)));
            Assert.assertEquals((char)0x0500, dregs.get(7).getValue());
            Assert.assertEquals((char)0xA000, dregs.get(0).getValue());
            Assert.assertTrue(freg.isSet(FlagRegister.OVERFLOW_FLAG));
        });
    }

    @Test
    public void handleInstructionPassingDoNotPassUnregistered() {
        exceptionLess(() -> {
            var processor = new CPU();
            var dregs = processor.getDataRegisters();
            var freg = processor.getFlagRegister();
            var alu = new ALU(freg, dregs.get(7));
            dregs.get(0).setValue((char)0x1002);
            dregs.get(1).setValue((char)0x5000);
            processor.execute(new Instruction(InstructionType.ALU_UMUL, dregs.get(0), dregs.get(1)));
            Assert.assertEquals((char)0x0000, dregs.get(7).getValue());
            Assert.assertEquals((char)0x1002, dregs.get(0).getValue());
            Assert.assertFalse(freg.isSet(FlagRegister.OVERFLOW_FLAG));
        });
    }

    @Test
    public void handleInstructionFiltering() {
        exceptionLess(() -> {
            var processor = new CPU();
            var dregs = processor.getDataRegisters();
            var freg = processor.getFlagRegister();
            var alu0 = new ALU(freg, dregs.get(6));
            var alu1 = new ALU(freg, dregs.get(7));
            processor.registerUnit(alu0, i -> i.getType().ordinal() >= InstructionType.ALU_ADD.ordinal()
                                           && i.getType().ordinal() <= InstructionType.ALU_SUB.ordinal());
            processor.registerUnit(alu1, i -> i.getType().ordinal() >= InstructionType.ALU_UMUL.ordinal()
                                           && i.getType().ordinal() <= InstructionType.ALU_SDIV.ordinal());
            var d0 = dregs.get(0);
            var d1 = dregs.get(1);
            var d6 = dregs.get(6);
            var d7 = dregs.get(7);
            d0.setValue((char) 0x0100);
            d1.setValue((char) 0x0010);

            processor.execute(new Instruction(InstructionType.ALU_ADD, d0, d1));
            Assert.assertEquals((char) 0x0110, d0.getValue());
            Assert.assertEquals((char) 0x0000, d6.getValue());
            Assert.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_UMUL, d0, d1));
            Assert.assertEquals((char) 0x1100, d0.getValue());
            Assert.assertEquals((char) 0x0000, d6.getValue());
            Assert.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_SUB, d0, d1));
            Assert.assertEquals((char) 0x10F0, d0.getValue());
            Assert.assertEquals((char) 0x0000, d6.getValue());
            Assert.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_UDIV, d0, d1));
            Assert.assertEquals((char) 0x010F, d0.getValue());
            Assert.assertEquals((char) 0x0000, d6.getValue());
            Assert.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_UDIV, d0, d1));
            Assert.assertEquals((char) 0x0010, d0.getValue());
            Assert.assertEquals((char) 0x0000, d6.getValue());
            Assert.assertEquals((char) 0x000F, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_SUB, d0, d1));
            Assert.assertEquals((char) 0x0000, d0.getValue());
            Assert.assertEquals((char) 0x0000, d6.getValue());
            Assert.assertEquals((char) 0x000F, d7.getValue());
        });
    }
}
