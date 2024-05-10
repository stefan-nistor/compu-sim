package ro.uaic.swqual.tester;

import ro.uaic.swqual.mem.RAM;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.proc.ALU;
import ro.uaic.swqual.proc.CPU;
import ro.uaic.swqual.proc.ClockDependent;
import ro.uaic.swqual.proc.IPU;
import ro.uaic.swqual.proc.MMU;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static ro.uaic.swqual.model.operands.FlagRegister.ILLEGAL_FLAG;

public class Tester implements Runnable {
    private static final String CHECKS_PATH = "src/test/resources/checks/";
    private final String path;
    private final Map<Expectation, Boolean> outcomes = new HashMap<>();
    private boolean globalOutcome;

    @Override
    public void run() {
        var parser = new TesterParser();
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

        if (parser.getExpectationMap().isEmpty()) {
            System.err.println("Error: no expectations found in '" + path + "'");
            globalOutcome = false;
            return;
        }

        simulate(parser, cpu, ipu, () -> pc.getValue() < instr.size() ? instr.get(pc.getValue()) : null);
        drawConclusions(parser.isExpectedToSucceed());
    }

    private void drawConclusions(boolean successExpected) {
        globalOutcome = true;
        if (!successExpected) {
            if (outcomes.values().stream().allMatch(v -> v)) {
                System.err.println("Outcome of test '" + path + "' invalid. Expected failure, but all expectations succeeded");
                globalOutcome = false;
            }
            return;
        } else {
            for (var expectationAndResult : outcomes.entrySet()) {
                if (Boolean.FALSE.equals(expectationAndResult.getValue())) {
                    System.out.println(
                            "Expectation '" + expectationAndResult.getKey().getCode() + "' at line "
                            + expectationAndResult.getKey().getLine() + " did not succeed. \n"
                            + expectationAndResult.getKey().dump()
                    );
                    globalOutcome = false;
                }
            }
        }

        if (globalOutcome) {
            System.out.println("Test '" + path + "' was successful");
        }
    }

    private void simulate(
            TesterParser parser,
            CPU cpu,
            ClockDependent stepper,
            Supplier<Instruction> currentInstruction
    ) {
        var freg = cpu.getFlagRegister();
        while (!freg.isSet(ILLEGAL_FLAG)) {
            var nextInstr = currentInstruction.get();
            var expMap = parser.getExpectationMap();
            var expectation = expMap.get(nextInstr);
            stepper.onTick();
            if (expectation != null) {
                outcomes.put(expectation, expectation.evaluate());
            }
        }
    }

    public Tester(String path) {
        this.path = path;
    }

    /**
     * Utility to validate .asm files in resources/checks
     * Run with the path of the test as the first parameter
     *   Example: ./Tester alu/reg_operations.asm
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: The tester must be run with a file path to simulate");
            System.exit(1);
        }

        new Tester(CHECKS_PATH + args[0]).run();
    }
}
