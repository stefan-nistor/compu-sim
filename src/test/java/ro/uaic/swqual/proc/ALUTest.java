package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

public class ALUTest extends ProcTestUtility {
    private interface ALUTestConsumer {
        void apply(ALU alu, FlagTestPredicate test, Register s) throws Throwable;
    }

    void aluTest(ALUTestConsumer r) throws Throwable {
        var flagReg = new FlagRegister();
        var specOutReg = new Register();
        flagReg.setValue((char)0);
        specOutReg.setValue((char)0);
        var alu = new ALU(flagReg, specOutReg);
        FlagTestPredicate test = (flags) -> {
            /// From my knowledge, impossible to resolve as stream with varargs, even with a safe <T> T[] toArray(T...) 
            char allFlags = 0;
            for (var flag : flags) {
                allFlags |= flag;
            }
            return allFlags == flagReg.getValue();
        };
        r.apply(alu, test, specOutReg);
    }

    @Test
    public void addRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(10);
            var r2 = new TestRegister(15);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            Assert.assertEquals(25, r1.getValue());
            Assert.assertEquals(0, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void addZeroFlagTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0);
            var r2 = new TestRegister(0);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            Assert.assertEquals(0, r1.getValue());
            Assert.assertEquals(0, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void addRegRegOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // -1
            var r2 = new TestRegister(0xFFFF); // -2
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            Assert.assertEquals((char)0xFFFE, r1.getValue());
            Assert.assertEquals(0, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void addRegRegNegativeTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // -1
            var r2 = new TestRegister(0xFFFE); // -2
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            Assert.assertEquals((char)0xFFFD, r1.getValue()); // -3
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void addRegRegNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFE); // -2
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            Assert.assertEquals((char)0xFFFF, r1.getValue()); // -1
            Assert.assertTrue(flagTest.test());
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void addRegRegNPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFE); // -2
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            Assert.assertEquals((char)0x0001, r1.getValue()); // 1
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            Assert.assertEquals((char)0x0001, r1.getValue());
            Assert.assertTrue(flagTest.test());
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegPPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            Assert.assertEquals((char)0xFFFF, r1.getValue()); // -1
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegPPNTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            Assert.assertEquals((char)0xFFFE, r1.getValue()); // -2
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegnNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            Assert.assertEquals((char)0xFFFA, r1.getValue()); // -6
            Assert.assertTrue(flagTest.test());
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegnNPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFFA); // -6
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            Assert.assertEquals((char)0x0002, r1.getValue());
            Assert.assertTrue(flagTest.test());
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void subRegRegnNPPTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFA); // -6
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            Assert.assertEquals((char)0xFFFE, r1.getValue()); // -2
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void umulRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0004);
            var r2 = new TestRegister(0x0008);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0x0020, r1.getValue());
            Assert.assertTrue(flagTest.test());
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void umulRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0004);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void umulRegRegZeroTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
            Assert.assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    public void umulRegRegOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0100);
            var r2 = new TestRegister(0x0102);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0x0200, r1.getValue());
            Assert.assertEquals((char)0x0001, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void umulRegRegOverflowTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x6000);
            var r2 = new TestRegister(0x0102);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0xC000, r1.getValue());
            Assert.assertEquals((char)0x0060, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void umulRegRegOverflowTestZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x1000);
            var r2 = new TestRegister(0x1000);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0x0100, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void umulRegRegNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // not -4, 65532
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0xFFFC, r1.getValue()); // 65532
            Assert.assertEquals(0, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void umulRegRegNNPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // 65532
            var r2 = new TestRegister(0xFFFC); // 65532
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            Assert.assertEquals((char)0x0010, r1.getValue());
            Assert.assertEquals((char)0xFFF8, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void smulRegRegPPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0010);
            var r2 = new TestRegister(0x0010);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            Assert.assertEquals((char)0x0100, r1.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void smulRegRegPPPOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0010);
            var r2 = new TestRegister(0x1000);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0x0001, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void smulRegRegNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            Assert.assertEquals((char)0xFFEC, r1.getValue()); // -20
            Assert.assertEquals((char)0xFFFF, specOut.getValue()); // remaining negative bits
            Assert.assertEquals((0xFFFFL << 16) + 0xFFEC, ((long) specOut.getValue() << 16) + r1.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void smulRegRegPNNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            Assert.assertEquals((char)0xFFEC, r1.getValue()); // -20
            Assert.assertEquals((char)0xFFFF, specOut.getValue()); // remaining negative bits
            Assert.assertEquals((0xFFFFL << 16) + 0xFFEC, ((long) specOut.getValue() << 16) + r1.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void smulRegRegPNNOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x7FFF); // 32767
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            Assert.assertEquals((char)0x0004, r1.getValue()); // -20
            Assert.assertEquals((char)0xFFFE, specOut.getValue()); // remaining negative bits + remainder of op
            Assert.assertEquals((0xFFFEL << 16) + 0x0004, ((long) specOut.getValue() << 16) + r1.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void smulRegRegNNPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            Assert.assertEquals((char)0x0010, r1.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void smulRegRegNNPOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x8004); // -32764
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            Assert.assertEquals((char)0xFFF0, r1.getValue());
            Assert.assertEquals((char)0x0001, specOut.getValue());
            // 131056
            Assert.assertEquals((0x0001L << 16) + 0xFFF0, ((long) specOut.getValue() << 16) + r1.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void udivRegRegPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0017); //23
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x0005, r1.getValue());
            Assert.assertEquals((char)0x0003, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void udivRegRegPPTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x7FFF); // 32767
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x7FFF, r1.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void udivRegRegPPTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 0x65535
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0xFFFF, r1.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void udivRegRegPPTest4() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 65535
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x7FFF, r1.getValue()); // 32767
            Assert.assertEquals((char)0x0001, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void udivRegRegPPTest5() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 65535
            var r2 = new TestRegister(0x7FFF); // 32767
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x0002, r1.getValue());
            Assert.assertEquals((char)0x0001, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void udivRegRegPPTest6() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 65535
            var r2 = new TestRegister(0x9FFF); // 40959
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x0001, r1.getValue());
            Assert.assertEquals((char)0x6000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void udivRegRegZeroFlag() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0x0002, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void udivRegRegZeroNoRemFlag() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void udivRegRegDivZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0014);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            Assert.assertEquals((char)0x0014, r1.getValue()); // unchanged
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.DIV_ZERO_FLAG));
        }));
    }

    @Test
    public void sdivRegRegPPTest1() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0001, r1.getValue());
            Assert.assertEquals((char)0x0002, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNPTest1() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF0); // -16
            var r2 = new TestRegister(0x0006);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0xFFFE, r1.getValue()); // -2
            Assert.assertEquals((char)0xFFFC, specOut.getValue()); // -4
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNPTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFEF); // -17
            var r2 = new TestRegister(0x0006);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0xFFFE, r1.getValue()); // -2
            Assert.assertEquals((char)0xFFFB, specOut.getValue()); // -5
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNPTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF4); // -12
            var r2 = new TestRegister(0x0006);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0xFFFE, r1.getValue()); // -2
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void sdivRegRegPNTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0020); // 32
            var r2 = new TestRegister(0xFFFB); // -5
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0xFFFA, r1.getValue()); // -6
            Assert.assertEquals((char)0x0002, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNPZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x000A); // 10
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0xFFFC, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegPNZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x000A); // 10
            var r2 = new TestRegister(0xFFF0); // -16
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0x000A, specOut.getValue()); // 10
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF1); // -15
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0003, r1.getValue());
            Assert.assertEquals((char)0xFFFD, specOut.getValue()); // -3
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNNZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFF1); // -15
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0xFFFC, specOut.getValue()); // -4
            Assert.assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    public void sdivRegRegPZeroFlagTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNZeroFlagTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0000, r1.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void sdivRegRegPZeroDivByZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0x0005, r1.getValue()); // unchanged
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.DIV_ZERO_FLAG));
        }));
    }

    @Test
    public void sdivRegRegNZeroDivByZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFB); // -5
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            Assert.assertEquals((char)0xFFFB, r1.getValue()); // unchanged
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.DIV_ZERO_FLAG));
        }));
    }

    @Test
    public void orRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0010);
            var r2 = new TestRegister(0x0021);
            alu.execute(new Instruction(InstructionType.ALU_OR, r1, r2));
            Assert.assertEquals((char) 0x0031, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void orRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_OR, r1, r2));
            Assert.assertEquals((char) 0x0000, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void orRegRegTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFEFE);
            var r2 = new TestRegister(0xEFEF);
            alu.execute(new Instruction(InstructionType.ALU_OR, r1, r2));
            Assert.assertEquals((char) 0xFFFF, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void andRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0610);
            var r2 = new TestRegister(0x0231);
            alu.execute(new Instruction(InstructionType.ALU_AND, r1, r2));
            Assert.assertEquals((char) 0x0210, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void andRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0001);
            var r2 = new TestRegister(0x0010);
            alu.execute(new Instruction(InstructionType.ALU_AND, r1, r2));
            Assert.assertEquals((char) 0x0000, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void andRegRegTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFEFE);
            var r2 = new TestRegister(0xEFEF);
            alu.execute(new Instruction(InstructionType.ALU_AND, r1, r2));
            Assert.assertEquals((char) 0xEEEE, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void xorRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0610);
            var r2 = new TestRegister(0x0231);
            alu.execute(new Instruction(InstructionType.ALU_XOR, r1, r2));
            Assert.assertEquals((char) 0x0421, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void xorRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0011);
            var r2 = new TestRegister(0x0011);
            alu.execute(new Instruction(InstructionType.ALU_XOR, r1, r2));
            Assert.assertEquals((char) 0x0000, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void xorRegRegTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFEFE);
            var r2 = new TestRegister(0xEFEF);
            alu.execute(new Instruction(InstructionType.ALU_XOR, r1, r2));
            Assert.assertEquals((char) 0x1111, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void notRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x1234);
            var r2 = new Register();
            alu.execute(new Instruction(InstructionType.ALU_NOT, r1, r2));
            Assert.assertEquals((char) 0xEDCB, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void notRegTestZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF);
            var r2 = new Register();
            alu.execute(new Instruction(InstructionType.ALU_NOT, r1, r2));
            Assert.assertEquals((char) 0x0000, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void shlRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0001);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            Assert.assertEquals((char) 0x0008, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void shlRegRegTestZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            Assert.assertEquals((char) 0x0000, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void shlRegRegTestTrim() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0FF0);
            var r2 = new TestRegister(0x0010); // 16
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            Assert.assertEquals((char) 0x0000, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void shlRegRegTestTrim2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0FF7);
            var r2 = new TestRegister(0x0008);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            Assert.assertEquals((char) 0xF700, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void shlRegRegTestTrim3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF7);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            Assert.assertEquals((char) 0xFF70, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void shlRegRegTestTrim4() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF7);
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            Assert.assertEquals((char) 0xFFDC, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void shrRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x00F0);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            Assert.assertEquals((char) 0x00F0, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void shrRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x00F0);
            var r2 = new TestRegister(0x0008);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            Assert.assertEquals((char) 0x0000, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    public void shrRegRegZeroTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0FF0);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            Assert.assertEquals((char) 0x00FF, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void shrRegRegZeroTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0F75);
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            Assert.assertEquals((char) 0x03DD, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void shrRegRegZeroTest4() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0F75);
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            Assert.assertEquals((char) 0x07BA, r1.getValue());
            Assert.assertEquals((char) 0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void compareRegRegPPEqTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            Assert.assertEquals((char)0x0005, r1.getValue());
            Assert.assertEquals((char)0x0005, r2.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.EQUAL_FLAG));
        }));
    }

    @Test
    public void compareRegRegPPLeTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0004);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            Assert.assertEquals((char)0x0004, r1.getValue());
            Assert.assertEquals((char)0x0005, r2.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    public void compareRegRegPPGtTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            Assert.assertEquals((char)0x0005, r1.getValue());
            Assert.assertEquals((char)0x0004, r2.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void compareRegRegNNEqTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFD); // -3
            var r2 = new TestRegister(0xFFFD);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            Assert.assertEquals((char)0xFFFD, r1.getValue());
            Assert.assertEquals((char)0xFFFD, r2.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.EQUAL_FLAG));
        }));
    }

    @Test
    public void compareRegRegNNLeTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFFD); // -3
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            Assert.assertEquals((char)0xFFFC, r1.getValue());
            Assert.assertEquals((char)0xFFFD, r2.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    public void compareRegRegNNGtTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFD); // -3
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            Assert.assertEquals((char)0xFFFD, r1.getValue());
            Assert.assertEquals((char)0xFFFC, r2.getValue());
            Assert.assertEquals((char)0x0000, specOut.getValue());
            Assert.assertTrue(flagTest.test());
        }));
    }

    @Test
    public void addOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_ADD, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_ADD, p1, p2));
            Assert.assertEquals((char)0xFFFF, p1.getValue());
        }));
    }

    @Test
    public void subOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SUB, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SUB, p1, p2));
            Assert.assertEquals((char)0xFE01, p1.getValue());
        }));
    }

    @Test
    public void smulOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0002);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, p1, p2));
            Assert.assertEquals((char)0x0008, p1.getValue());
        }));
    }

    @Test
    public void umulOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0002);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, p1, p2));
            Assert.assertEquals((char)0x0008, p1.getValue());
        }));
    }

    @Test
    public void sdivOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0004);
            var p2 = new Constant((char)0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, p1, p2));
            Assert.assertEquals((char)0x0002, p1.getValue());
        }));
    }

    @Test
    public void udivOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0004);
            var p2 = new Constant((char)0x0002);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, p1, p2));
            Assert.assertEquals((char)0x0002, p1.getValue());
        }));
    }

    @Test
    public void orOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_OR, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF55);
            var p2 = new Constant((char)0x55FF);
            alu.execute(new Instruction(InstructionType.ALU_OR, p1, p2));
            Assert.assertEquals((char)0xFFFF, p1.getValue());
        }));
    }

    @Test
    public void andOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_AND, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF55);
            var p2 = new Constant((char)0x55FF);
            alu.execute(new Instruction(InstructionType.ALU_AND, p1, p2));
            Assert.assertEquals((char)0x5555, p1.getValue());
        }));
    }

    @Test
    public void xorOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_XOR, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF55);
            var p2 = new Constant((char)0x55FF);
            alu.execute(new Instruction(InstructionType.ALU_XOR, p1, p2));
            Assert.assertEquals((char)0xAAAA, p1.getValue());
        }));
    }

    @Test
    public void shlOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SHL, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x00F0);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHL, p1, p2));
            Assert.assertEquals((char)0x0F00, p1.getValue());
        }));
    }

    @Test
    public void shrOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SHR, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x00F0);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHR, p1, p2));
            Assert.assertEquals((char)0x000F, p1.getValue());
        }));
    }

    @Test
    public void notOutParamSupport() {
        Assert.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x0000);
            alu.execute(new Instruction(InstructionType.ALU_NOT, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x00F0);
            var p2 = new Constant((char)0x0000);
            alu.execute(new Instruction(InstructionType.ALU_NOT, p1, p2));
            Assert.assertEquals((char)0xFF0F, p1.getValue());
        }));
    }

    @Test
    public void cmpOutParamSupport() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0x00F0);
            var p2 = new Constant((char)0x00F0);
            alu.execute(new Instruction(InstructionType.ALU_CMP, p1, p2));
            Assert.assertTrue(flagTest.test(FlagRegister.EQUAL_FLAG));
        }));
    }

    @Test
    public void illegalALUInstruction() {
        Assert.assertThrows(
                InstructionException.class,
                () -> aluTest((alu, flag, out) -> alu.execute(jmp(1))
        ));
    }
}
