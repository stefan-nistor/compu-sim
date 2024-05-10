package ro.uaic.swqual.integration.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.Parser;
import ro.uaic.swqual.mem.RAM;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.proc.ALU;
import ro.uaic.swqual.proc.CPU;
import ro.uaic.swqual.proc.ClockDependent;
import ro.uaic.swqual.proc.IPU;
import ro.uaic.swqual.proc.MMU;
import ro.uaic.swqual.tester.TesterParser;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.uaic.swqual.model.operands.FlagRegister.ILLEGAL_FLAG;

class TesterParserIntegrationTest {
    interface ParserIntegrationConsumer<T extends Parser> {
        void accept(T parser, CPU cpu, ClockDependent stepper, Supplier<Instruction> getCurrentInstruction);
    }

    <T extends Parser> void parse(String path, T parser, ParserIntegrationConsumer<T> consumer) {
        var instr = parser.parse(path).link().getInstructions();
        var cpu = new CPU();
        var freg = cpu.getFlagRegister();
        var dregs = cpu.getDataRegisters();
        var pc = cpu.getProgramCounter();
        var sp = cpu.getStackPointer();
        parser.resolveReferences(cpu.getRegistryReferenceMap());
        var ipu = new IPU(instr, freg, pc);
        var mmu = new MMU(freg, sp);
        var alu = new ALU(freg, dregs.get(7));
        cpu.registerExecutor(alu);
        cpu.registerExecutor(ipu);
        cpu.registerExecutor(mmu);
        cpu.registerLocator(mmu);

        mmu.registerExecutor(cpu);
        alu.registerLocator(cpu);
        ipu.registerLocator(cpu);

        ipu.subscribe(cpu);

        try {
            var ram = new RAM(0x10000, freg); // 65536
            mmu.registerHardwareUnit(ram, (char) 0, addr -> true); // any values pass since it is max size
        } catch (Exception e) {
            // do nothing
        }

        consumer.accept(parser, cpu, ipu, () -> pc.getValue() < instr.size() ? instr.get(pc.getValue()) : null);
    }

    @Test
    void expectedBehaviorTestShouldResolveValidEvaluations() {
        var resource0 = "src/test/resources/integration/tester-parser-actual-simulation-test.txt";
        parse(resource0, new TesterParser(), (parser, cpu, stepper, getCurrentInstruction) -> {
            var freg = cpu.getFlagRegister();
            while (!freg.isSet(ILLEGAL_FLAG)) {
                var nextInstr = getCurrentInstruction.get();
                var expMap = parser.getExpectationMap();
                var expectation = expMap.get(nextInstr);
                stepper.onTick();
                if (expectation != null) {
                    assertTrue(expectation.evaluate());
                }
            }
        });
    }

    @Test
    void expectedBehaviorFailsWhenExpectedToFail() {
        var resource0 = "src/test/resources/integration/tester-parser-actual-simulation-neg-test.txt";
        parse(resource0, new TesterParser(), (parser, cpu, stepper, getCurrentInstruction) -> {
            var freg = cpu.getFlagRegister();
            while (!freg.isSet(ILLEGAL_FLAG)) {
                var nextInstr = getCurrentInstruction.get();
                var expMap = parser.getExpectationMap();
                var expectation = expMap.get(nextInstr);
                stepper.onTick();
                if (expectation != null) {
                    assertFalse(expectation.evaluate());
                }
            }
        });
    }
}
