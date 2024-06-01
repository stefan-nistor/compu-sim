package ro.uaic.swqual.tester;

import ro.uaic.swqual.mem.RandomAccessMemory;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.peripheral.Display;
import ro.uaic.swqual.model.peripheral.Keyboard;
import ro.uaic.swqual.proc.ArithmeticLogicUnit;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.proc.ClockListener;
import ro.uaic.swqual.proc.InputOutputManagementUnit;
import ro.uaic.swqual.proc.InstructionProcessingUnit;
import ro.uaic.swqual.proc.MemoryManagementUnit;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ro.uaic.swqual.model.operands.FlagRegister.ILLEGAL_FLAG;

/**
 * Represents a specialized testing framework, containing a built-in simulator. <br/>
 * It is able to parse source files through an extended {@link TesterParser} and discern truths about the state
 * of the simulator using {@link Expectation} objects extracted from checker assembly code
 * (e.g. mov r0 5; // expect-true {r0 == 5}). <br/>
 * The expectations contain state storage code, providing an informative dump if the expectation and actual results
 * differ. <br/>
 * <br/>
 * This can be run independently as the class provides a {@link Tester#main} entry point or as a {@link Runnable}. <br/>
 * By default, the unit tests will run all files present at {@link Tester#CHECKS_PATH a default path} in a
 * {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor} (test/.../tester/TesterChecksTest.java)
 */
public class Tester implements Runnable {
    /** The default path of the Tester Framework assembly check files */
    public static final String CHECKS_PATH = "src/test/resources/checks/";
    /** The path of the file currently being evaluated */
    private final String path;
    /** Outcomes of each expectation */
    private final Map<Expectation, Boolean> outcomes = new HashMap<>();
    /** Global test outcome */
    private boolean globalOutcome;
    /** Execution output consumer */
    private final Consumer<String> out;
    /** Execution error consumer */
    private final Consumer<String> err;


    /* Arbitrary values used in the simulated environment */
    private static final char MMU_IOMU_OFFSET = 0x0;
    private static final char MMU_ADDRESS_RANGE = 0x100;

    private static final char MMU_RAM_OFFSET = 0x100;
    private static final char MMU_RAM_SIZE = 0xFF00;

    private static final char IOMU_KB_OFFSET = 0x10;
    private static final char IOMU_KB_SIZE = 0x2;

    private static final char IOMU_DISP_OFFSET = 0x20;
    private static final char IOMU_DISP_SIZE = 0x30;

    /**
     * Final outcome getter
     * @return true if outcome is successful, false otherwise
     * */
    public boolean getOutcome() {
        return globalOutcome;
    }

    /**
     * Method called when execution of the tester starts. <br/>
     * This will parse the file for instructions and expectations, simulate the code in a local processor and
     * evaluate expectations when appropriate. <br/>
     * In the end, it will evaluate the outcome and dump differential information.
     */
    @Override
    public void run() {
        var parser = new TesterParser();

        var kb = new Keyboard();
        // when parsing kb-preload, pass it to the local keyboard peripheral
        parser.addOnKbPreloadListener(parameters -> parameters.forEach(p -> kb.press(p.getValue())));

        var instr = parser.parse(path).link().getInstructions();
        var cpu = new CentralProcessingUnit();
        var freg = cpu.getFlagRegister();
        var dregs = cpu.getDataRegisters();
        var pc = cpu.getProgramCounter();
        var sp = cpu.getStackPointer();
        sp.setValue(MMU_RAM_OFFSET); // start SP at beginning of RAM
        parser.resolveReferences(cpu.getRegistryReferenceMap());
        var ipu = new InstructionProcessingUnit(instr, freg, pc, sp);
        var mmu = new MemoryManagementUnit(freg, sp);
        // use register r7 for ALU overflows
        var alu = new ArithmeticLogicUnit(freg, dregs.get(7));

        // register executors and locators as
        // cpu -> layer0 -> layer1 -> ...
        // layer0 -> cpu
        // layer1 -> cpu
        // ...

        cpu.registerExecutor(alu);
        cpu.registerExecutor(ipu);
        cpu.registerExecutor(mmu);
        cpu.registerLocator(mmu);


        mmu.registerExecutor(cpu);
        alu.registerLocator(cpu);
        ipu.registerLocator(cpu);
        ipu.registerExecutor(cpu);

        // and finally, since ipu is the root clock stepper:
        // ipu -> cpu
        ipu.subscribe(cpu);

        ipu.registerClockListener(cpu);
        cpu.registerClockListener(mmu);
        cpu.registerClockListener(alu);
        // Never link cpu back to ipu with ClockListener

        var iomu = new InputOutputManagementUnit(freg);
        var disp = new Display(IOMU_DISP_SIZE, freg);
        iomu.registerHardwareUnit(kb, IOMU_KB_OFFSET, IOMU_KB_SIZE);
        iomu.registerHardwareUnit(disp, IOMU_DISP_OFFSET, IOMU_DISP_SIZE);

        mmu.registerLocator(iomu, MMU_IOMU_OFFSET, MMU_ADDRESS_RANGE);
        mmu.registerClockListener(iomu);

        try {
            var ram = new RandomAccessMemory(MMU_RAM_SIZE, freg);
            mmu.registerHardwareUnit(ram, MMU_RAM_OFFSET, addr -> addr >= MMU_RAM_OFFSET);
            parser.readAddressesFrom(ram, MMU_RAM_OFFSET, (char) (MMU_RAM_SIZE - 1));
        } catch (Exception e) {
            // do nothing
        }

        // if no expectations found, fail early
        if (parser.getExpectationMap().isEmpty()) {
            err.accept("Error: no expectations found in '" + path + "'");
            globalOutcome = false;
            return;
        }

        for (var expectation : parser.getExpectationMap().values()) {
            // look for 'expect-display' expectations to link them to the local display peripheral
            if (expectation instanceof PredicateExpectation predicateExpectation
                    && predicateExpectation.getTag().equals("expect-display")) {
                predicateExpectation.setCallback(expectedOutput -> Objects.equals(expectedOutput, disp.getText()));
                predicateExpectation.setDumpHintSupplier(disp::getText);
            }
        }

        // start simulating execution
        simulate(parser, cpu, ipu, () -> pc.getValue() < instr.size() ? instr.get(pc.getValue()) : null);
        // after which, draw conclusions
        drawConclusions(parser.isExpectedToSucceed());
    }

    /**
     * Method used to invoke the actual simulation. Will run the root clock listener until failure is raised in the cpu,
     * specifically {@link ro.uaic.swqual.model.operands.FlagRegister#ILLEGAL_FLAG FlagRegister.ILLEGAL_FLAG},
     * signifying end of instruction list.
     * @param parser the parser that was used to acquire the instructions. Used to extract expectations from
     * @param cpu the cpu simulating the code, used to get the flag register
     * @param stepper the root clock listener (ipu) that will pass the clock signal along
     * @param currentInstruction supplier returning the next instruction to be run (to obtain associated expectation)
     */
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
            assert expMap != null;
            var expectation = expMap.get(nextInstr);
            stepper.onTick();
            if (expectation != null) {
                outcomes.put(expectation, expectation.evaluate());
            }
        }
    }

    /**
     * Method used to generate a differential view when the global expectation and global actual outcome mismatch.
     * @param successExpected expected global outcome
     */
    private void drawConclusions(boolean successExpected) {
        globalOutcome = true;
        if (!successExpected) {
            // if failure expected, and all expectation are successful, fail.
            if (outcomes.values().stream().allMatch(v -> v)) {
                err.accept("Outcome of test '" + path + "' invalid. Expected failure, but all expectations succeeded");
                globalOutcome = false;
            }
        } else {
            // if success expected, and any expectation failed, generate a dump
            // sort the outcomes of the failed expectations by line and generate a dump for each
            // passing it to the error consumer.
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

        // if everything was successful, report it.
        if (globalOutcome) {
            out.accept("Test '" + path + "' was successful");
        }
    }

    /**
     * Primary constructor
     * @param path path to acquire the test file from
     * @param out the execution output consumer
     * @param err the execution error consumer
     */
    public Tester(String path, Consumer<String> out, Consumer<String> err) {
        assert path != null;
        assert out != null;
        assert err != null;
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
