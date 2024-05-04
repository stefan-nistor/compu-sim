package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

interface ALUTestConsumer {
    void apply(ALU alu, FlagRegister f, Register s) throws Throwable;
}

public class ALUTest {
    void exceptionLess(ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    void aluTest(ALUTestConsumer r) throws Throwable {
        var flagReg = new FlagRegister();
        var specOutReg = new Register();
        flagReg.setValue((short)0);
        specOutReg.setValue((short)0);
        var alu = new ALU(flagReg, specOutReg);
        r.apply(alu, flagReg, specOutReg);
    }

    @Test
    public void addRegRegTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(10);
            r2.setValue(15);
            alu.execute(InstructionType.ALU_ADD, r1, r2);
            Assert.assertEquals(25, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void addZeroFlagTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0);
            r2.setValue(0);
            alu.execute(InstructionType.ALU_ADD, r1, r2);
            Assert.assertEquals(0, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertTrue(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void addRegRegOverflowTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFF);
            r2.setValue(0xFFFF);
            alu.execute(InstructionType.ALU_ADD, r1, r2);
            Assert.assertEquals((short)0xFFFE, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void addRegRegNegativeTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFF); // -1
            r2.setValue(0xFFFE); // -2
            alu.execute(InstructionType.ALU_ADD, r1, r2);
            Assert.assertEquals((short)0xFFFD, r1.getValue()); // -3
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void addRegRegNPNTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFE); // -2
            r2.setValue(0x0001); // 1
            alu.execute(InstructionType.ALU_ADD, r1, r2);
            Assert.assertEquals((short)0xFFFF, r1.getValue()); // -1
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void addRegRegNPPTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFE); // -2
            r2.setValue(0x0003); // 3
            alu.execute(InstructionType.ALU_ADD, r1, r2);
            Assert.assertEquals((short)0x0001, r1.getValue()); // 1
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0002);
            r2.setValue(0x0001);
            alu.execute(InstructionType.ALU_SUB, r1, r2);
            Assert.assertEquals((short)0x0001, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0002);
            r2.setValue(0x0002);
            alu.execute(InstructionType.ALU_SUB, r1, r2);
            Assert.assertEquals((short)0x0000, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertTrue(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegPPNTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0002);
            r2.setValue(0x0003);
            alu.execute(InstructionType.ALU_SUB, r1, r2);
            Assert.assertEquals((short)0xFFFF, r1.getValue()); // -1
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegPPNTest2() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0002);
            r2.setValue(0x0004);
            alu.execute(InstructionType.ALU_SUB, r1, r2);
            Assert.assertEquals((short)0xFFFE, r1.getValue()); // -2
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegnNPNTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFC); // -4
            r2.setValue(0x0002);
            alu.execute(InstructionType.ALU_SUB, r1, r2);
            Assert.assertEquals((short)0xFFFA, r1.getValue()); // -6
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegnNPPTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFC); // -4
            r2.setValue(0xFFFA); // -6
            alu.execute(InstructionType.ALU_SUB, r1, r2);
            Assert.assertEquals((short)0x0002, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegnNPPTest2() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFA); // -6
            r2.setValue(0xFFFC); // -4
            alu.execute(InstructionType.ALU_SUB, r1, r2);
            Assert.assertEquals((short)0xFFFE, r1.getValue()); // -2
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void mulRegRegTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0004);
            r2.setValue(0x0008);
            alu.execute(InstructionType.ALU_MUL, r1, r2);
            Assert.assertEquals((short)0x0020, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void mulRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0004);
            r2.setValue(0x0000);
            alu.execute(InstructionType.ALU_MUL, r1, r2);
            Assert.assertEquals((short)0x0000, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertTrue(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void mulRegRegZeroTest2() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0000);
            r2.setValue(0x0004);
            alu.execute(InstructionType.ALU_MUL, r1, r2);
            Assert.assertEquals((short)0x0000, r1.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertTrue(flag.isSet(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void mulRegRegOverflowTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0100);
            r2.setValue(0x0102);
            alu.execute(InstructionType.ALU_MUL, r1, r2);
            Assert.assertEquals((short)0x0200, r1.getValue());
            Assert.assertEquals((short)0x0001, specOut.getValue());
            Assert.assertTrue(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void mulRegRegOverflowTest2() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x6000);
            r2.setValue(0x0102);
            alu.execute(InstructionType.ALU_MUL, r1, r2);
            Assert.assertEquals((short)0xC000, r1.getValue());
            Assert.assertEquals((short)0x0060, specOut.getValue());
            Assert.assertTrue(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void mulRegRegOverflowTestZero() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x1000);
            r2.setValue(0x1000);
            alu.execute(InstructionType.ALU_MUL, r1, r2);
            Assert.assertEquals((short)0x0000, r1.getValue());
            Assert.assertEquals((short)0x0100, specOut.getValue());
            Assert.assertTrue(flag.isSet(FlagRegister.OVERFLOW_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void compareRegRegPPEqTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0005);
            r2.setValue(0x0005);
            alu.execute(InstructionType.ALU_CMP, r1, r2);
            Assert.assertEquals((short)0x0005, r1.getValue());
            Assert.assertEquals((short)0x0005, r2.getValue());
            Assert.assertEquals((short)0x0000, specOut.getValue());
            Assert.assertTrue(flag.isSet(FlagRegister.EQUAL_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    public void compareRegRegPPLeTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0004);
            r2.setValue(0x0005);
            alu.execute(InstructionType.ALU_CMP, r1, r2);
            Assert.assertEquals((short)0x0004, r1.getValue());
            Assert.assertEquals((short)0x0005, r2.getValue());
            Assert.assertEquals((short)0x0000, specOut.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.EQUAL_FLAG));
            Assert.assertTrue(flag.isSet(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    public void compareRegRegPPGtTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0x0005);
            r2.setValue(0x0004);
            alu.execute(InstructionType.ALU_CMP, r1, r2);
            Assert.assertEquals((short)0x0005, r1.getValue());
            Assert.assertEquals((short)0x0004, r2.getValue());
            Assert.assertEquals((short)0x0000, specOut.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.EQUAL_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    public void compareRegRegNNEqTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFD); // -3
            r2.setValue(0xFFFD); // -3
            alu.execute(InstructionType.ALU_CMP, r1, r2);
            Assert.assertEquals((short)0xFFFD, r1.getValue());
            Assert.assertEquals((short)0xFFFD, r2.getValue());
            Assert.assertEquals((short)0x0000, specOut.getValue());
            Assert.assertTrue(flag.isSet(FlagRegister.EQUAL_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    public void compareRegRegNNLeTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFC); // -4
            r2.setValue(0xFFFD); // -3
            alu.execute(InstructionType.ALU_CMP, r1, r2);
            Assert.assertEquals((short)0xFFFC, r1.getValue());
            Assert.assertEquals((short)0xFFFD, r2.getValue());
            Assert.assertEquals((short)0x0000, specOut.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.EQUAL_FLAG));
            Assert.assertTrue(flag.isSet(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    public void compareRegRegNNGtTest() {
        exceptionLess(() -> aluTest((alu, flag, specOut) -> {
            var r1 = new Register();
            var r2 = new Register();
            r1.setValue(0xFFFD); // -3
            r2.setValue(0xFFFC); // -4
            alu.execute(InstructionType.ALU_CMP, r1, r2);
            Assert.assertEquals((short)0xFFFD, r1.getValue());
            Assert.assertEquals((short)0xFFFC, r2.getValue());
            Assert.assertEquals((short)0x0000, specOut.getValue());
            Assert.assertFalse(flag.isSet(FlagRegister.EQUAL_FLAG));
            Assert.assertFalse(flag.isSet(FlagRegister.LESS_FLAG));
        }));
    }
}
