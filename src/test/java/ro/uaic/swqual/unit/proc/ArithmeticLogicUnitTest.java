package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.ArithmeticLogicUnit;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;

class ArithmeticLogicUnitTest implements ProcTestUtility {
    private interface ALUTestConsumer {
        void apply(ArithmeticLogicUnit alu, FlagTestPredicate test, Register s) throws Throwable;
    }

    void aluTest(ALUTestConsumer r) throws Throwable {
        var flagReg = new FlagRegister();
        var specOutReg = new Register();
        flagReg.setValue((char)0);
        specOutReg.setValue((char)0);
        var alu = new ArithmeticLogicUnit(flagReg, specOutReg);
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
    void addRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(10);
            var r2 = new TestRegister(15);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            assertEquals(25, r1.getValue());
            assertEquals(0, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void addZeroFlagTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0);
            var r2 = new TestRegister(0);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            assertEquals(0, r1.getValue());
            assertEquals(0, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void addRegRegOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // -1
            var r2 = new TestRegister(0xFFFF); // -2
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            assertEquals((char)0xFFFE, r1.getValue());
            assertEquals(0, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void addRegRegNegativeTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // -1
            var r2 = new TestRegister(0xFFFE); // -2
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            assertEquals((char)0xFFFD, r1.getValue()); // -3
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void addRegRegNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFE); // -2
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            assertEquals((char)0xFFFF, r1.getValue()); // -1
            assertTrue(flagTest.test());
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void addRegRegNPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFE); // -2
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_ADD, r1, r2));
            assertEquals((char)0x0001, r1.getValue()); // 1
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void subRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            assertEquals((char)0x0001, r1.getValue());
            assertTrue(flagTest.test());
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void subRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void subRegRegPPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            assertEquals((char)0xFFFF, r1.getValue()); // -1
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void subRegRegPPNTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            assertEquals((char)0xFFFE, r1.getValue()); // -2
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void subRegRegnNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            assertEquals((char)0xFFFA, r1.getValue()); // -6
            assertTrue(flagTest.test());
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void subRegRegnNPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFFA); // -6
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            assertEquals((char)0x0002, r1.getValue());
            assertTrue(flagTest.test());
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void subRegRegnNPPTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFA); // -6
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SUB, r1, r2));
            assertEquals((char)0xFFFE, r1.getValue()); // -2
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void umulRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0004);
            var r2 = new TestRegister(0x0008);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0x0020, r1.getValue());
            assertTrue(flagTest.test());
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void umulRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0004);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void umulRegRegZeroTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
            assertEquals(0, specOut.getValue());
        }));
    }

    @Test
    void umulRegRegOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0100);
            var r2 = new TestRegister(0x0102);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0x0200, r1.getValue());
            assertEquals((char)0x0001, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void umulRegRegOverflowTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x6000);
            var r2 = new TestRegister(0x0102);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0xC000, r1.getValue());
            assertEquals((char)0x0060, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void umulRegRegOverflowTestZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x1000);
            var r2 = new TestRegister(0x1000);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0x0100, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void umulRegRegNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // not -4, 65532
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0xFFFC, r1.getValue()); // 65532
            assertEquals(0, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void umulRegRegNNPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // 65532
            var r2 = new TestRegister(0xFFFC); // 65532
            alu.execute(new Instruction(InstructionType.ALU_UMUL, r1, r2));
            assertEquals((char)0x0010, r1.getValue());
            assertEquals((char)0xFFF8, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void smulRegRegPPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0010);
            var r2 = new TestRegister(0x0010);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            assertEquals((char)0x0100, r1.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void smulRegRegPPPOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0010);
            var r2 = new TestRegister(0x1000);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0x0001, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void smulRegRegNPNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            assertEquals((char)0xFFEC, r1.getValue()); // -20
            assertEquals((char)0xFFFF, specOut.getValue()); // remaining negative bits
            assertEquals((0xFFFFL << 16) + 0xFFEC, ((long) specOut.getValue() << 16) + r1.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void smulRegRegPNNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            assertEquals((char)0xFFEC, r1.getValue()); // -20
            assertEquals((char)0xFFFF, specOut.getValue()); // remaining negative bits
            assertEquals((0xFFFFL << 16) + 0xFFEC, ((long) specOut.getValue() << 16) + r1.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void smulRegRegPNNOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x7FFF); // 32767
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            assertEquals((char)0x0004, r1.getValue()); // -20
            assertEquals((char)0xFFFE, specOut.getValue()); // remaining negative bits + remainder of op
            assertEquals((0xFFFEL << 16) + 0x0004, ((long) specOut.getValue() << 16) + r1.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void smulRegRegNNPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            assertEquals((char)0x0010, r1.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void smulRegRegNNPOverflowTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x8004); // -32764
            alu.execute(new Instruction(InstructionType.ALU_SMUL, r1, r2));
            assertEquals((char)0xFFF0, r1.getValue());
            assertEquals((char)0x0001, specOut.getValue());
            // 131056
            assertEquals((0x0001L << 16) + 0xFFF0, ((long) specOut.getValue() << 16) + r1.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void udivRegRegPPTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0017); //23
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x0005, r1.getValue());
            assertEquals((char)0x0003, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void udivRegRegPPTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x7FFF); // 32767
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x7FFF, r1.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void udivRegRegPPTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 0x65535
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0xFFFF, r1.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void udivRegRegPPTest4() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 65535
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x7FFF, r1.getValue()); // 32767
            assertEquals((char)0x0001, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void udivRegRegPPTest5() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 65535
            var r2 = new TestRegister(0x7FFF); // 32767
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x0002, r1.getValue());
            assertEquals((char)0x0001, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void udivRegRegPPTest6() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF); // 65535
            var r2 = new TestRegister(0x9FFF); // 40959
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x0001, r1.getValue());
            assertEquals((char)0x6000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void udivRegRegZeroFlag() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0002);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0x0002, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void udivRegRegZeroNoRemFlag() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void udivRegRegDivZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0014);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, r1, r2));
            assertEquals((char)0x0014, r1.getValue()); // unchanged
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.DIV_ZERO_FLAG));
        }));
    }

    @Test
    void sdivRegRegPPTest1() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0001, r1.getValue());
            assertEquals((char)0x0002, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegNPTest1() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF0); // -16
            var r2 = new TestRegister(0x0006);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0xFFFE, r1.getValue()); // -2
            assertEquals((char)0xFFFC, specOut.getValue()); // -4
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegNPTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFEF); // -17
            var r2 = new TestRegister(0x0006);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0xFFFE, r1.getValue()); // -2
            assertEquals((char)0xFFFB, specOut.getValue()); // -5
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegNPTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF4); // -12
            var r2 = new TestRegister(0x0006);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0xFFFE, r1.getValue()); // -2
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void sdivRegRegPNTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0020); // 32
            var r2 = new TestRegister(0xFFFB); // -5
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0xFFFA, r1.getValue()); // -6
            assertEquals((char)0x0002, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegNPZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0x000A); // 10
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0xFFFC, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegPNZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x000A); // 10
            var r2 = new TestRegister(0xFFF0); // -16
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0x000A, specOut.getValue()); // 10
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegNNTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF1); // -15
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0003, r1.getValue());
            assertEquals((char)0xFFFD, specOut.getValue()); // -3
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegNNZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFF1); // -15
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0xFFFC, specOut.getValue()); // -4
            assertTrue(flagTest.test(FlagRegister.OVERFLOW_FLAG));
        }));
    }

    @Test
    void sdivRegRegPZeroFlagTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void sdivRegRegNZeroFlagTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0000, r1.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void sdivRegRegPZeroDivByZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0x0005, r1.getValue()); // unchanged
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.DIV_ZERO_FLAG));
        }));
    }

    @Test
    void sdivRegRegNZeroDivByZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFB); // -5
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, r1, r2));
            assertEquals((char)0xFFFB, r1.getValue()); // unchanged
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.DIV_ZERO_FLAG));
        }));
    }

    @Test
    void orRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0010);
            var r2 = new TestRegister(0x0021);
            alu.execute(new Instruction(InstructionType.ALU_OR, r1, r2));
            assertEquals((char) 0x0031, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void orRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_OR, r1, r2));
            assertEquals((char) 0x0000, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void orRegRegTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFEFE);
            var r2 = new TestRegister(0xEFEF);
            alu.execute(new Instruction(InstructionType.ALU_OR, r1, r2));
            assertEquals((char) 0xFFFF, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void andRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0610);
            var r2 = new TestRegister(0x0231);
            alu.execute(new Instruction(InstructionType.ALU_AND, r1, r2));
            assertEquals((char) 0x0210, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void andRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0001);
            var r2 = new TestRegister(0x0010);
            alu.execute(new Instruction(InstructionType.ALU_AND, r1, r2));
            assertEquals((char) 0x0000, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void andRegRegTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFEFE);
            var r2 = new TestRegister(0xEFEF);
            alu.execute(new Instruction(InstructionType.ALU_AND, r1, r2));
            assertEquals((char) 0xEEEE, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void xorRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0610);
            var r2 = new TestRegister(0x0231);
            alu.execute(new Instruction(InstructionType.ALU_XOR, r1, r2));
            assertEquals((char) 0x0421, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void xorRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0011);
            var r2 = new TestRegister(0x0011);
            alu.execute(new Instruction(InstructionType.ALU_XOR, r1, r2));
            assertEquals((char) 0x0000, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void xorRegRegTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFEFE);
            var r2 = new TestRegister(0xEFEF);
            alu.execute(new Instruction(InstructionType.ALU_XOR, r1, r2));
            assertEquals((char) 0x1111, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void notRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x1234);
            var r2 = new Register();
            alu.execute(new Instruction(InstructionType.ALU_NOT, r1, r2));
            assertEquals((char) 0xEDCB, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void notRegTestZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFF);
            var r2 = new Register();
            alu.execute(new Instruction(InstructionType.ALU_NOT, r1, r2));
            assertEquals((char) 0x0000, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void shlRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0001);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            assertEquals((char) 0x0008, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void shlRegRegTestZero() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0000);
            var r2 = new TestRegister(0x0003);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            assertEquals((char) 0x0000, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void shlRegRegTestTrim() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0FF0);
            var r2 = new TestRegister(0x0010); // 16
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            assertEquals((char) 0x0000, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void shlRegRegTestTrim2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0FF7);
            var r2 = new TestRegister(0x0008);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            assertEquals((char) 0xF700, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void shlRegRegTestTrim3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF7);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            assertEquals((char) 0xFF70, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void shlRegRegTestTrim4() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFF7);
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SHL, r1, r2));
            assertEquals((char) 0xFFDC, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void shrRegRegTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x00F0);
            var r2 = new TestRegister(0x0000);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            assertEquals((char) 0x00F0, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void shrRegRegZeroTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x00F0);
            var r2 = new TestRegister(0x0008);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            assertEquals((char) 0x0000, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.ZERO_FLAG));
        }));
    }

    @Test
    void shrRegRegZeroTest2() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0FF0);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            assertEquals((char) 0x00FF, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void shrRegRegZeroTest3() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0F75);
            var r2 = new TestRegister(0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            assertEquals((char) 0x03DD, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void shrRegRegZeroTest4() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0F75);
            var r2 = new TestRegister(0x0001);
            alu.execute(new Instruction(InstructionType.ALU_SHR, r1, r2));
            assertEquals((char) 0x07BA, r1.getValue());
            assertEquals((char) 0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void compareRegRegPPEqTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            assertEquals((char)0x0005, r1.getValue());
            assertEquals((char)0x0005, r2.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.EQUAL_FLAG));
        }));
    }

    @Test
    void compareRegRegPPLeTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0004);
            var r2 = new TestRegister(0x0005);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            assertEquals((char)0x0004, r1.getValue());
            assertEquals((char)0x0005, r2.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    void compareRegRegPPGtTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0x0005);
            var r2 = new TestRegister(0x0004);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            assertEquals((char)0x0005, r1.getValue());
            assertEquals((char)0x0004, r2.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void compareRegRegNNEqTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFD); // -3
            var r2 = new TestRegister(0xFFFD);
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            assertEquals((char)0xFFFD, r1.getValue());
            assertEquals((char)0xFFFD, r2.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.EQUAL_FLAG));
        }));
    }

    @Test
    void compareRegRegNNLeTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFC); // -4
            var r2 = new TestRegister(0xFFFD); // -3
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            assertEquals((char)0xFFFC, r1.getValue());
            assertEquals((char)0xFFFD, r2.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test(FlagRegister.LESS_FLAG));
        }));
    }

    @Test
    void compareRegRegNNGtTest() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var r1 = new TestRegister(0xFFFD); // -3
            var r2 = new TestRegister(0xFFFC); // -4
            alu.execute(new Instruction(InstructionType.ALU_CMP, r1, r2));
            assertEquals((char)0xFFFD, r1.getValue());
            assertEquals((char)0xFFFC, r2.getValue());
            assertEquals((char)0x0000, specOut.getValue());
            assertTrue(flagTest.test());
        }));
    }

    @Test
    void addOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_ADD, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_ADD, p1, p2));
            assertEquals((char)0xFFFF, p1.getValue());
        }));
    }

    @Test
    void subOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SUB, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SUB, p1, p2));
            assertEquals((char)0xFE01, p1.getValue());
        }));
    }

    @Test
    void smulOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0002);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SMUL, p1, p2));
            assertEquals((char)0x0008, p1.getValue());
        }));
    }

    @Test
    void umulOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0002);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_UMUL, p1, p2));
            assertEquals((char)0x0008, p1.getValue());
        }));
    }

    @Test
    void sdivOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0004);
            var p2 = new Constant((char)0x0002);
            alu.execute(new Instruction(InstructionType.ALU_SDIV, p1, p2));
            assertEquals((char)0x0002, p1.getValue());
        }));
    }

    @Test
    void udivOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x0004);
            var p2 = new Constant((char)0x0002);
            alu.execute(new Instruction(InstructionType.ALU_UDIV, p1, p2));
            assertEquals((char)0x0002, p1.getValue());
        }));
    }

    @Test
    void orOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_OR, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF55);
            var p2 = new Constant((char)0x55FF);
            alu.execute(new Instruction(InstructionType.ALU_OR, p1, p2));
            assertEquals((char)0xFFFF, p1.getValue());
        }));
    }

    @Test
    void andOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_AND, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF55);
            var p2 = new Constant((char)0x55FF);
            alu.execute(new Instruction(InstructionType.ALU_AND, p1, p2));
            assertEquals((char)0x5555, p1.getValue());
        }));
    }

    @Test
    void xorOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_XOR, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0xFF55);
            var p2 = new Constant((char)0x55FF);
            alu.execute(new Instruction(InstructionType.ALU_XOR, p1, p2));
            assertEquals((char)0xAAAA, p1.getValue());
        }));
    }

    @Test
    void shlOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SHL, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x00F0);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHL, p1, p2));
            assertEquals((char)0x0F00, p1.getValue());
        }));
    }

    @Test
    void shrOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x00FF);
            alu.execute(new Instruction(InstructionType.ALU_SHR, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x00F0);
            var p2 = new Constant((char)0x0004);
            alu.execute(new Instruction(InstructionType.ALU_SHR, p1, p2));
            assertEquals((char)0x000F, p1.getValue());
        }));
    }

    @Test
    void notOutParamSupport() {
        Assertions.assertThrows(ParameterException.class, () -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0xFF00);
            var p2 = new Constant((char)0x0000);
            alu.execute(new Instruction(InstructionType.ALU_NOT, p1, p2));
        }));
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new TestRegister((char)0x00F0);
            var p2 = new Constant((char)0x0000);
            alu.execute(new Instruction(InstructionType.ALU_NOT, p1, p2));
            assertEquals((char)0xFF0F, p1.getValue());
        }));
    }

    @Test
    void cmpOutParamSupport() {
        exceptionLess(() -> aluTest((alu, flagTest, specOut) -> {
            var p1 = new Constant((char)0x00F0);
            var p2 = new Constant((char)0x00F0);
            alu.execute(new Instruction(InstructionType.ALU_CMP, p1, p2));
            assertTrue(flagTest.test(FlagRegister.EQUAL_FLAG));
        }));
    }

    @Test
    void illegalALUInstruction() {
        Assertions.assertThrows(
                InstructionException.class,
                () -> aluTest((alu, flag, out) -> alu.execute(jmp(1))
        ));
    }

    @Test
    void aluDefaultFilterShouldOnlyAcceptAluOps() {
        var alu = new ArithmeticLogicUnit(freg(), reg());
        var pred = alu.getDefaultFilter();
        assertEquals(
                InstructionType.ALU_ADD,
                Stream.of(
                        InstructionType.MMU_MOV,
                        InstructionType.ALU_ADD,
                        InstructionType.IPU_JMP
                )
                        .filter(i -> pred.test(new Instruction(i)))
                        .sorted() // force eval of all filters
                        .findAny().orElse(InstructionType.LABEL)
        );
    }

    @Test
    void aluRaiseFlagShouldRaiseFlag() {
        var freg = freg();
        var alu = new ArithmeticLogicUnit(freg, reg());
        alu.raiseFlag(SEG_FLAG);
        assertTrue(freg.isSet(SEG_FLAG));
    }
}
