package ro.uaic.swqual.tester;

import ro.uaic.swqual.mem.RandomAccessMemory;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.proc.ArithmeticLogicUnit;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.proc.ClockListener;
import ro.uaic.swqual.proc.InstructionProcessingUnit;
import ro.uaic.swqual.proc.MemoryManagementUnit;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ro.uaic.swqual.model.operands.FlagRegister.ILLEGAL_FLAG;

public class Tester implements Runnable {
    public static final String CHECKS_PATH = "src/test/resources/checks/";
    private final String path;
    private final Map<Expectation, Boolean> outcomes = new HashMap<>();
    private boolean globalOutcome;
    private final Consumer<String> out;
    private final Consumer<String> err;

    public boolean getOutcome() {
        return globalOutcome;
    }

    @Override
    public void run() {
        var parser = new TesterParser();
        var instr = parser.parse(path).link().getInstructions();
        var cpu = new CentralProcessingUnit();
        var freg = cpu.getFlagRegister();
        var dregs = cpu.getDataRegisters();
        var pc = cpu.getProgramCounter();
        var sp = cpu.getStackPointer();
        parser.resolveReferences(cpu.getRegistryReferenceMap());
        var ipu = new InstructionProcessingUnit(instr, freg, pc);
        var mmu = new MemoryManagementUnit(freg, sp);
        var alu = new ArithmeticLogicUnit(freg, dregs.get(7));
        cpu.registerExecutor(alu);
        cpu.registerExecutor(ipu);
        cpu.registerExecutor(mmu);
        cpu.registerLocator(mmu);

        mmu.registerExecutor(cpu);
        alu.registerLocator(cpu);
        ipu.registerLocator(cpu);

        ipu.subscribe(cpu);

        ipu.registerClockListener(cpu);
        cpu.registerClockListener(mmu);
        cpu.registerClockListener(alu);
        // Never link cpu back to ipu with ClockListener

        try {
            var ram = new RandomAccessMemory(0x10000, freg); // 65536
            mmu.registerHardwareUnit(ram, (char) 0, addr -> true); // any values pass since it is max size
        } catch (Exception e) {
            // do nothing
        }

        if (parser.getExpectationMap().isEmpty()) {
            err.accept("Error: no expectations found in '" + path + "'");
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
                err.accept("Outcome of test '" + path + "' invalid. Expected failure, but all expectations succeeded");
                globalOutcome = false;
            }
        } else {
            outcomes.entrySet().stream().sorted(Comparator.comparingInt(l -> l.getKey().getLine())).forEach(
                    expectationAndResult -> {
                        if (Boolean.FALSE.equals(expectationAndResult.getValue())) {
                            err.accept(
                                    "Expectation '" + expectationAndResult.getKey().getCode() + "' at line "
                                            + expectationAndResult.getKey().getLine() + " did not succeed. \n"
                                            + expectationAndResult.getKey().dump()
                            );
                            globalOutcome = false;
                        }
                    }
            );
        }

        if (globalOutcome) {
            out.accept("Test '" + path + "' was successful");
        }
    }

    private void simulate(
            TesterParser parser,
            CentralProcessingUnit cpu,
            ClockListener stepper,
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

    public Tester(String path, Consumer<String> out, Consumer<String> err) {
        this.path = path;
        this.out = out;
        this.err = err;
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

        new Tester(CHECKS_PATH + args[0], System.out::println, System.err::println).run();
    }
}
