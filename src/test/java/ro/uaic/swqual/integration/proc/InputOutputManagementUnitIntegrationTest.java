package ro.uaic.swqual.integration.proc;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.Parser;
import ro.uaic.swqual.mem.RandomAccessMemory;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.peripheral.Keyboard;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.proc.ClockListener;
import ro.uaic.swqual.proc.InputOutputManagementUnit;
import ro.uaic.swqual.proc.InstructionProcessingUnit;
import ro.uaic.swqual.proc.MemoryManagementUnit;
import ro.uaic.swqual.unit.mem.MemTestUtility;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputOutputManagementUnitIntegrationTest implements MemTestUtility, ProcTestUtility {
    private static final char KB_ADDR = (char) 0x20;

    interface InputOutputManagementUnitIntegrationTestConsumer {
        void accept(CentralProcessingUnit cpu, MemoryManagementUnit mmu, ClockListener stepper);
    }

    void inputOutputManagementUnitIntegrationTest(
            List<Instruction> instructions,
            InputOutputManagementUnitIntegrationTestConsumer consumer
    ) {
        var cpu = new CentralProcessingUnit();
        var freg = cpu.getFlagRegister();
        var pc = cpu.getProgramCounter();
        var sp = cpu.getStackPointer();
        var parser = new Parser();
        parser.getInstructions().addAll(instructions);
        parser.resolveReferences(cpu.getRegistryReferenceMap());
        var ipu = new InstructionProcessingUnit(instructions, freg, pc, sp);
        var mmu = new MemoryManagementUnit(freg, sp);
        cpu.registerExecutor(ipu);
        cpu.registerExecutor(mmu);

        // Never register IPU to the CPU as a clock listener.
        // This will create a short-circuit/circular-dependency/stack-overflow scenario
        cpu.registerClockListener(mmu);

        mmu.registerExecutor(cpu);

        ipu.subscribe(cpu);
        var ram = new RandomAccessMemory((char) 0xFF00, freg);
        mmu.registerHardwareUnit(ram, (char) 0x0100, (char) 0xFF00);
        consumer.accept(cpu, mmu, ipu);
    }

    @Test
    void keyboardIntegrationShouldSucceed() {
        inputOutputManagementUnitIntegrationTest(
                List.of(
                        mov(ref("r0"), aloc(_const(KB_ADDR))),
                        mov(ref("r0"), aloc(_const(KB_ADDR))),
                        mov(ref("r0"), aloc(_const(KB_ADDR)))
                ),
                (cpu, mmu, stepper) -> {
                    var iomu = new InputOutputManagementUnit(cpu.getFlagRegister());
                    var kb = new Keyboard();
                    mmu.registerLocator(iomu, (char) 0, (char) 0x100);
                    iomu.registerHardwareUnit(kb, KB_ADDR, (char) 0x02);
                    kb.press((char) 0x50);
                    kb.press((char) 0x51);
                    kb.press((char) 0x52);
                    mmu.registerClockListener(iomu);

                    var r0 = cpu.getDataRegisters().getFirst();

                    stepper.onTick();
                    assertEquals((char) 0x50, r0.getValue());
                    stepper.onTick();
                    assertEquals((char) 0x51, r0.getValue());
                    stepper.onTick();
                    assertEquals((char) 0x52, r0.getValue());
                }
        );
    }
}
