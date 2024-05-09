package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.ALU;
import ro.uaic.swqual.proc.CPU;
import ro.uaic.swqual.proc.ClockDependent;
import ro.uaic.swqual.proc.IPU;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;

class CPUTest implements ProcTestUtility {
    @Test
    void processorDataRegSize() {
        var processor = new CPU();
        var dataRegs = processor.getDataRegisters();
        Assertions.assertEquals(8, dataRegs.size());
    }

    @Test
    void processorDataRegStore() {
        try {
            var processor = new CPU();
            var dataRegs = processor.getDataRegisters();
            var reg1 = dataRegs.get(3);
            var reg2 = dataRegs.get(5);
            reg1.setValue(1234);
            reg2.setValue(5678);
            Assertions.assertEquals(1234, reg1.getValue());
            Assertions.assertEquals(5678, reg2.getValue());
            Assertions.assertEquals(1234, processor.getDataRegisters().get(3).getValue());
            Assertions.assertEquals(5678, processor.getDataRegisters().get(5).getValue());
        } catch (ValueException exception) {
            Assertions.fail(exception.getMessage());
        }
    }

    @Test
    void handleAllInstructions() {
        // Sonar: FP S2699 - Does not check inside for assertions.
        //      exceptionLess does assert on exception.
        // This check inside functions is already done in S5961
        //      so this can be backported, unless it is SE and not AST.
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
    void handleInstructionPassing() {
        exceptionLess(() -> {
            var processor = new CPU();
            var dregs = processor.getDataRegisters();
            var freg = processor.getFlagRegister();
            var alu0 = new ALU(freg, dregs.get(7));
            processor.registerExecutor(alu0);
            dregs.get(0).setValue((char)0x1002);
            dregs.get(1).setValue((char)0x5000);
            processor.execute(new Instruction(InstructionType.ALU_UMUL, dregs.get(0), dregs.get(1)));
            Assertions.assertEquals((char)0x0500, dregs.get(7).getValue());
            Assertions.assertEquals((char)0xA000, dregs.get(0).getValue());
            assertTrue(freg.isSet(FlagRegister.OVERFLOW_FLAG));
        });
    }

    @Test
    void handleInstructionPassingDoNotPassUnregistered() {
        exceptionLess(() -> {
            var processor = new CPU();
            var dregs = processor.getDataRegisters();
            var freg = processor.getFlagRegister();
            var alu = new ALU(freg, dregs.get(7));
            dregs.get(0).setValue((char)0x1002);
            dregs.get(1).setValue((char)0x5000);
            processor.execute(new Instruction(InstructionType.ALU_UMUL, dregs.get(0), dregs.get(1)));
            Assertions.assertEquals((char)0x0000, dregs.get(7).getValue());
            Assertions.assertEquals((char)0x1002, dregs.get(0).getValue());
            Assertions.assertFalse(freg.isSet(FlagRegister.OVERFLOW_FLAG));
        });
    }

    @Test
    void handleInstructionFiltering() {
        exceptionLess(() -> {
            var processor = new CPU();
            var dregs = processor.getDataRegisters();
            var freg = processor.getFlagRegister();
            var alu0 = new ALU(freg, dregs.get(6));
            var alu1 = new ALU(freg, dregs.get(7));
            processor.registerExecutor(alu0, i -> i.getType().ordinal() >= InstructionType.ALU_ADD.ordinal()
                                               && i.getType().ordinal() <= InstructionType.ALU_SUB.ordinal());
            processor.registerExecutor(alu1, i -> i.getType().ordinal() >= InstructionType.ALU_UMUL.ordinal()
                                               && i.getType().ordinal() <= InstructionType.ALU_SDIV.ordinal());
            var d0 = dregs.get(0);
            var d1 = dregs.get(1);
            var d6 = dregs.get(6);
            var d7 = dregs.get(7);
            d0.setValue((char) 0x0100);
            d1.setValue((char) 0x0010);

            processor.execute(new Instruction(InstructionType.ALU_ADD, d0, d1));
            Assertions.assertEquals((char) 0x0110, d0.getValue());
            Assertions.assertEquals((char) 0x0000, d6.getValue());
            Assertions.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_UMUL, d0, d1));
            Assertions.assertEquals((char) 0x1100, d0.getValue());
            Assertions.assertEquals((char) 0x0000, d6.getValue());
            Assertions.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_SUB, d0, d1));
            Assertions.assertEquals((char) 0x10F0, d0.getValue());
            Assertions.assertEquals((char) 0x0000, d6.getValue());
            Assertions.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_UDIV, d0, d1));
            Assertions.assertEquals((char) 0x010F, d0.getValue());
            Assertions.assertEquals((char) 0x0000, d6.getValue());
            Assertions.assertEquals((char) 0x0000, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_UDIV, d0, d1));
            Assertions.assertEquals((char) 0x0010, d0.getValue());
            Assertions.assertEquals((char) 0x0000, d6.getValue());
            Assertions.assertEquals((char) 0x000F, d7.getValue());
            processor.execute(new Instruction(InstructionType.ALU_SUB, d0, d1));
            Assertions.assertEquals((char) 0x0000, d0.getValue());
            Assertions.assertEquals((char) 0x0000, d6.getValue());
            Assertions.assertEquals((char) 0x000F, d7.getValue());
        });
    }

    private interface CpuAluIpuConsumer {
        void apply(CPU cpu, ClockDependent stepper) throws Throwable;
    }

    private static class RegisterReference extends Parameter {
        private final String name;

        public RegisterReference(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private void cpuAluIpuTest(
            List<Instruction> instructions,
            int aluOutIdx,
            BiFunction<CPU, RegisterReference, Register> registerMapping,
            CpuAluIpuConsumer consumer
    ) throws Throwable {
        var cpu = new CPU();
        var dregs = cpu.getDataRegisters();
        var flags = cpu.getFlagRegister();
        var pc = cpu.getProgramCounter();
        var ipu = new IPU(instructions.stream().peek(instruction -> {
            if (instruction.getParam1() instanceof RegisterReference) {
                instruction.setParam1(registerMapping.apply(cpu, (RegisterReference) instruction.getParam1()));
            }
            if (instruction.getParam2() instanceof RegisterReference) {
                instruction.setParam2(registerMapping.apply(cpu, (RegisterReference) instruction.getParam2()));
            }
        }).toList(), flags, pc);
        cpu.registerExecutor(new ALU(flags, dregs.get(aluOutIdx)));
        cpu.registerExecutor(ipu);
        ipu.subscribe(cpu);
        consumer.apply(cpu, ipu);
    }

    RegisterReference ref(String name) {
        return new RegisterReference(name);
    }

    @Test
    void cpuModulesAluIpu() {
        exceptionLess(() -> cpuAluIpuTest(
                List.of(
                        add(ref("r0"), _const(10)),
                        // @StartLoop:
                        add(ref("r1"), _const(2)),
                        sub(ref("r0"), _const(1)),
                        cmp(ref("r0"), _const(0)),
                        jne(1), // @StartLoop
                        umul(ref("r1"), _const(5))
                ),
                7,
                (cpu, reference) -> {
                    final var regMap = Map.of(
                            "r0", cpu.getDataRegisters().get(0),
                            "r1", cpu.getDataRegisters().get(1)
                    );
                    return regMap.get(reference.getName());
                },
                (cpu, stepper) -> {
                    var flags = cpu.getFlagRegister();
                    while (!flags.isSet(FlagRegister.ILLEGAL_FLAG)) {
                        stepper.onTick();
                    }
                    Assertions.assertEquals((char) 100, cpu.getDataRegisters().get(1).getValue());
                }
        ));
    }

    @Test
    void cpuRaiseFlagShouldRaiseFlag() {
        var cpu = new CPU();
        var freg = cpu.getFlagRegister();
        cpu.raiseFlag(SEG_FLAG);
        assertTrue(freg.isSet(SEG_FLAG));
    }

    @Test
    void cpuShouldHaveValidStackPointer() {
        var cpu = new CPU();
        var sp = cpu.getStackPointer();
        assertInstanceOf(Register.class, sp);
    }
}
