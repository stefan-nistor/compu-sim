package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

import java.util.List;

public class IPUTest extends ProcTest {
    private interface IPUTestConsumer {
        void apply(IPU ipu, FlagTestPredicate test, Register pc, List<Instruction> instructions) throws Throwable;
    }

    private interface IPUTestWithFlagConsumer {
        void apply(IPU ipu, FlagRegister flag, Register pc, List<Instruction> instructions) throws Throwable;
    }

    void ipuTest(List<Instruction> instructions, IPUTestConsumer consumer) throws Throwable {
        var flagReg = new FlagRegister();
        var pc = new Register();
        var ipu = new IPU(instructions, flagReg, pc);

        pc.setValue((char)0);
        FlagTestPredicate test = (flags) -> {
            char allFlags = 0;
            for (var flag : flags) {
                allFlags |= flag;
            }
            return allFlags == flagReg.getValue();
        };
        consumer.apply(ipu, test, pc, instructions);
    }

    void ipuTestWithFlagEdit(List<Instruction> instructions, IPUTestWithFlagConsumer consumer) throws Throwable {
        var flagReg = new FlagRegister();
        var pc = new Register();
        var ipu = new IPU(instructions, flagReg, pc);

        pc.setValue((char)0);
        consumer.apply(ipu, flagReg, pc, instructions);
    }

    @Test
    public void subscribeTest() {
        exceptionLess(() -> {
            var reg0 = new TestRegister((char)10);
            var reg1 = new TestRegister((char)20);
            var reg2 = new TestRegister((char)30);
            ipuTest(List.of(
                    add(reg0, reg1),
                    add(reg0, reg2),
                    add(reg2, reg1),
                    add(reg1, reg0),
                    add(reg1, reg2),
                    jmp((char)3)
            ), (ipu, test, pc, instructions) -> {
                var cpu = new ProcessingUnit() {
                    Instruction lastExecuted = null;
                    @Override
                    public void execute(Instruction instruction) throws InstructionException, ParameterException {
                        if (ipu.getDefaultFilter().test(instruction)) {
                            ipu.execute(instruction);
                        }
                        lastExecuted = instruction;
                    }
                };
                ipu.subscribe(cpu);

                Assert.assertEquals(instructions.getFirst(), ipu.next());
                Assert.assertNull(cpu.lastExecuted);
                Assert.assertEquals(instructions.getFirst(), ipu.next());
                Assert.assertNull(cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertEquals(instructions.get(1), ipu.next());
                Assert.assertEquals(instructions.get(0), cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertEquals(instructions.get(2), ipu.next());
                Assert.assertEquals(instructions.get(1), cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertEquals(instructions.get(3), ipu.next());
                Assert.assertEquals(instructions.get(2), cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertEquals(instructions.get(4), ipu.next());
                Assert.assertEquals(instructions.get(3), cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertEquals(instructions.get(5), ipu.next());
                Assert.assertEquals(instructions.get(4), cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertEquals(instructions.get(3), ipu.next());
                Assert.assertEquals(instructions.get(5), cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertEquals(instructions.get(4), ipu.next());
                Assert.assertEquals(instructions.get(3), cpu.lastExecuted);
                Assert.assertTrue(test.test());
            });
        });
    }

    @Test
    public void outOfRangeTest() {
        exceptionLess(() -> {
            var reg0 = new TestRegister((char)10);
            var reg1 = new TestRegister((char)20);
            ipuTest(List.of(
                    add(reg0, reg1)
            ), (ipu, test, pc, instructions) -> {
                var cpu = new ProcessingUnit() {
                    Instruction lastExecuted = null;
                    @Override
                    public void execute(Instruction instruction) throws InstructionException, ParameterException {
                        if (ipu.getDefaultFilter().test(instruction)) {
                            ipu.execute(instruction);
                        }
                        lastExecuted = instruction;
                    }
                };
                ipu.subscribe(cpu);

                Assert.assertEquals(instructions.getFirst(), ipu.next());
                Assert.assertNull(cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();

                Assert.assertEquals(IPU.defaultInstruction, ipu.next());
                Assert.assertEquals(instructions.getFirst(), cpu.lastExecuted);
                Assert.assertTrue(test.test());
                ipu.step();
                Assert.assertTrue(test.test(FlagRegister.ILLEGAL_FLAG));
                Assert.assertEquals(instructions.getFirst(), ipu.next());
                Assert.assertEquals(IPU.defaultInstruction, cpu.lastExecuted);
                ipu.step();
                Assert.assertTrue(test.test(FlagRegister.ILLEGAL_FLAG));
                Assert.assertEquals(IPU.defaultInstruction, ipu.next());
                Assert.assertEquals(instructions.getFirst(), cpu.lastExecuted);
            });
        });
    }

    @Test
    public void jeqTest() {
        exceptionLess(() -> ipuTestWithFlagEdit(List.of(
                jeq((char)2),
                jmp((char)0),
                jmp((char)0)
        ), (ipu, flags, pc, instructions) -> {
            var cpu = new ProcessingUnit() {
                Instruction lastExecuted = null;
                @Override
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    if (ipu.getDefaultFilter().test(instruction)) {
                        ipu.execute(instruction);
                    }
                    lastExecuted = instruction;
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());
        }));
    }

    @Test
    public void jneTest() {
        exceptionLess(() -> ipuTestWithFlagEdit(List.of(
                jne((char)2),
                jmp((char)0),
                jmp((char)0)
        ), (ipu, flags, pc, instructions) -> {
            var cpu = new ProcessingUnit() {
                Instruction lastExecuted = null;
                @Override
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    if (ipu.getDefaultFilter().test(instruction)) {
                        ipu.execute(instruction);
                    }
                    lastExecuted = instruction;
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());
        }));
    }

    @Test
    public void jltTest() {
        exceptionLess(() -> ipuTestWithFlagEdit(List.of(
                jlt((char)2),
                jmp((char)0),
                jmp((char)0)
        ), (ipu, flags, pc, instructions) -> {
            var cpu = new ProcessingUnit() {
                Instruction lastExecuted = null;
                @Override
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    if (ipu.getDefaultFilter().test(instruction)) {
                        ipu.execute(instruction);
                    }
                    lastExecuted = instruction;
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.LESS_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());
        }));
    }

    @Test
    public void jgtTest() {
        exceptionLess(() -> ipuTestWithFlagEdit(List.of(
                jgt((char)2),
                jmp((char)0),
                jmp((char)0)
        ), (ipu, flags, pc, instructions) -> {
            var cpu = new ProcessingUnit() {
                Instruction lastExecuted = null;
                @Override
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    if (ipu.getDefaultFilter().test(instruction)) {
                        ipu.execute(instruction);
                    }
                    lastExecuted = instruction;
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.LESS_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            flags.set(FlagRegister.LESS_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());
        }));
    }

    @Test
    public void jleTest() {
        exceptionLess(() -> ipuTestWithFlagEdit(List.of(
                jle((char)2),
                jmp((char)0),
                jmp((char)0)
        ), (ipu, flags, pc, instructions) -> {
            var cpu = new ProcessingUnit() {
                Instruction lastExecuted = null;
                @Override
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    if (ipu.getDefaultFilter().test(instruction)) {
                        ipu.execute(instruction);
                    }
                    lastExecuted = instruction;
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.LESS_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());
        }));
    }

    @Test
    public void jgeTest() {
        exceptionLess(() -> ipuTestWithFlagEdit(List.of(
                jge((char)2),
                jmp((char)0),
                jmp((char)0)
        ), (ipu, flags, pc, instructions) -> {
            var cpu = new ProcessingUnit() {
                Instruction lastExecuted = null;
                @Override
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    if (ipu.getDefaultFilter().test(instruction)) {
                        ipu.execute(instruction);
                    }
                    lastExecuted = instruction;
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.LESS_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            flags.set(FlagRegister.LESS_FLAG);
            Assert.assertEquals(instructions.getFirst(), ipu.next());
            ipu.step();
            Assert.assertEquals(instructions.get(2), ipu.next());
        }));
    }
}
