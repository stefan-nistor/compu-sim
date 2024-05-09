package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.IPU;
import ro.uaic.swqual.proc.ProcessingUnit;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;

class IPUTest implements ProcTestUtility {
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
    void subscribeTest() {
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

                    @Override
                    public void raiseFlag(char value) {
                        discard(value);
                    }
                };
                ipu.subscribe(cpu);

                Assertions.assertEquals(instructions.getFirst(), ipu.next());
                Assertions.assertNull(cpu.lastExecuted);
                Assertions.assertEquals(instructions.getFirst(), ipu.next());
                Assertions.assertNull(cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                Assertions.assertEquals(instructions.get(1), ipu.next());
                Assertions.assertEquals(instructions.get(0), cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                Assertions.assertEquals(instructions.get(2), ipu.next());
                Assertions.assertEquals(instructions.get(1), cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                Assertions.assertEquals(instructions.get(3), ipu.next());
                Assertions.assertEquals(instructions.get(2), cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                Assertions.assertEquals(instructions.get(4), ipu.next());
                Assertions.assertEquals(instructions.get(3), cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                Assertions.assertEquals(instructions.get(5), ipu.next());
                Assertions.assertEquals(instructions.get(4), cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                Assertions.assertEquals(instructions.get(3), ipu.next());
                Assertions.assertEquals(instructions.get(5), cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                Assertions.assertEquals(instructions.get(4), ipu.next());
                Assertions.assertEquals(instructions.get(3), cpu.lastExecuted);
                assertTrue(test.test());
            });
        });
    }

    @Test
    void outOfRangeTest() {
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

                    @Override
                    public void raiseFlag(char value) {
                        discard(value);
                    }
                };
                ipu.subscribe(cpu);

                Assertions.assertEquals(instructions.getFirst(), ipu.next());
                Assertions.assertNull(cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();

                Assertions.assertEquals(IPU.defaultInstruction, ipu.next());
                Assertions.assertEquals(instructions.getFirst(), cpu.lastExecuted);
                assertTrue(test.test());
                ipu.onTick();
                assertTrue(test.test(FlagRegister.ILLEGAL_FLAG));
                Assertions.assertEquals(instructions.getFirst(), ipu.next());
                Assertions.assertEquals(IPU.defaultInstruction, cpu.lastExecuted);
                ipu.onTick();
                assertTrue(test.test(FlagRegister.ILLEGAL_FLAG));
                Assertions.assertEquals(IPU.defaultInstruction, ipu.next());
                Assertions.assertEquals(instructions.getFirst(), cpu.lastExecuted);
            });
        });
    }

    @Test
    void jeqTest() {
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

                @Override
                public void raiseFlag(char value) {
                    flags.set(value);
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());
        }));
    }

    @Test
    void jneTest() {
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

                @Override
                public void raiseFlag(char value) {
                    flags.set(value);
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());
        }));
    }

    @Test
    void jltTest() {
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

                @Override
                public void raiseFlag(char value) {
                    flags.set(value);
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.LESS_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());
        }));
    }

    @Test
    void jgtTest() {
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

                @Override
                public void raiseFlag(char value) {
                    flags.set(value);
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.LESS_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            flags.set(FlagRegister.LESS_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());
        }));
    }

    @Test
    void jleTest() {
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

                @Override
                public void raiseFlag(char value) {
                    flags.set(value);
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.LESS_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());
        }));
    }

    @Test
    void jgeTest() {
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

                @Override
                public void raiseFlag(char value) {
                    flags.set(value);
                }
            };
            ipu.subscribe(cpu);

            ipu.reset();
            flags.clear();
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.LESS_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(1), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());

            ipu.reset();
            flags.clear();
            flags.set(FlagRegister.EQUAL_FLAG);
            flags.set(FlagRegister.LESS_FLAG);
            Assertions.assertEquals(instructions.getFirst(), ipu.next());
            ipu.onTick();
            Assertions.assertEquals(instructions.get(2), ipu.next());
        }));
    }

    @Test
    void illegalIPUInstruction() {
        final List<Instruction> emptyList = List.of();
        Assertions.assertThrows(InstructionException.class, () -> ipuTest(
                emptyList, (ipu, flags, pc, instructions) -> ipu.execute(
                        add(new Register(), _const(0))
                )
        ));
    }

    @Test
    void ipuRaiseFlagShouldRaiseFlag() {
        var freg = freg();
        var ipu = new IPU(List.of(), freg, reg());
        ipu.raiseFlag(SEG_FLAG);
        assertTrue(freg.isSet(SEG_FLAG));
    }

    @Test
    void ipuDefaultFilterShouldAcceptOnlyIpuInstructions() {
        var ipu = new IPU(List.of(), freg(), reg());
        var pred = ipu.getDefaultFilter();
        assertEquals(
                InstructionType.IPU_JMP,
                Stream.of(
                        InstructionType.ALU_ADD,
                        InstructionType.IPU_JMP,
                        InstructionType.LABEL
                )
                        .filter(i -> pred.test(new Instruction(i)))
                        .sorted() // force eval of all filters
                        .findAny().orElse(InstructionType.LABEL)
        );
    }
}
