package ro.uaic.swqual.swing;

import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.mem.RandomAccessMemory;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.peripheral.Display;
import ro.uaic.swqual.model.peripheral.Keyboard;
import ro.uaic.swqual.proc.ArithmeticLogicUnit;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.proc.InputOutputManagementUnit;
import ro.uaic.swqual.proc.InstructionProcessingUnit;
import ro.uaic.swqual.proc.MemoryManagementUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CpuOrchestrator {
    private enum State {
        RUNNING,
        STOPPED
    }

    public static final String RAM_ADDRESS_KEY = "RamAddress";
    public static final String RAM_SIZE_KEY = "RamSize";
    public static final String KEYBOARD_ADDRESS_KEY = "KeyboardAddress";
    public static final String DISPLAY_ADDRESS_KEY = "DisplayAddress";
    public static final String DISPLAY_SIZE_KEY = "DisplaySize";

    public static final Character DEFAULT_RAM_ADDRESS = 0x100;
    public static final Character DEFAULT_RAM_SIZE = 0xFC00;
    public static final Character DEFAULT_KEYBOARD_ADDRESS = 0x10;
    public static final Character KEYBOARD_SIZE = 0x02;
    public static final Character DEFAULT_DISPLAY_ADDRESS = 0x20;
    public static final Character DEFAULT_DISPLAY_SIZE = 0x30;

    private State state = State.STOPPED;

    private final CentralProcessingUnit centralProcessingUnit;
    private final InstructionProcessingUnit instructionProcessingUnit;
    private final MemoryManagementUnit memoryManagementUnit;
    private final ArithmeticLogicUnit arithmeticLogicUnit;
    private final RandomAccessMemory randomAccessMemory;
    private final InputOutputManagementUnit inputOutputManagementUnit;
    private final Keyboard keyboard;
    private final Display display;
    private final List<Runnable> onUpdateListeners = new ArrayList<>();

    private final Object lock = new Object();
    private final Thread cpuClock;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public CentralProcessingUnit getCentralProcessingUnit() {
        return centralProcessingUnit;
    }

    public InputOutputManagementUnit getInputOutputManagementUnit() {
        return inputOutputManagementUnit;
    }

    public InstructionProcessingUnit getInstructionProcessingUnit() {
        return instructionProcessingUnit;
    }

    public MemoryManagementUnit getMemoryManagementUnit() {
        return memoryManagementUnit;
    }

    public ArithmeticLogicUnit getArithmeticLogicUnit() {
        return arithmeticLogicUnit;
    }

    public RandomAccessMemory getRandomAccessMemory() {
        return randomAccessMemory;
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public Display getDisplay() {
        return display;
    }

    public CpuOrchestrator(Map<String, Character> configParameters) {
        centralProcessingUnit = new CentralProcessingUnit();

        var flagRegister = centralProcessingUnit.getFlagRegister();
        var dataRegisters = centralProcessingUnit.getDataRegisters();
        var programCounter = centralProcessingUnit.getProgramCounter();
        var stackPointer = centralProcessingUnit.getStackPointer();

        instructionProcessingUnit =
                new InstructionProcessingUnit(List.of(), flagRegister, programCounter, stackPointer);
        memoryManagementUnit = new MemoryManagementUnit(flagRegister, stackPointer);
        arithmeticLogicUnit = new ArithmeticLogicUnit(flagRegister, dataRegisters.getLast());
        inputOutputManagementUnit = new InputOutputManagementUnit(flagRegister);

        var ramAddress = configParameters.getOrDefault(RAM_ADDRESS_KEY, DEFAULT_RAM_ADDRESS);
        var ramSize = configParameters.getOrDefault(RAM_SIZE_KEY, DEFAULT_RAM_SIZE);
        if (ramSize % 0x400 != 0) {
            throw new ParameterException("Invalid RAM configuration size. Must be a multiple of 1024");
        }

        randomAccessMemory = new RandomAccessMemory(ramSize, flagRegister);

        keyboard = new Keyboard();

        var displayAddress = configParameters.getOrDefault(DISPLAY_ADDRESS_KEY, DEFAULT_DISPLAY_ADDRESS);
        var displaySize = configParameters.getOrDefault(DISPLAY_SIZE_KEY, DEFAULT_DISPLAY_SIZE);
        if (displayAddress + displaySize + 1 >= ramAddress) {
            throw new ParameterException("Overlapping objects: Display overlaps RAM");
        }

        display = new Display(displaySize, flagRegister);


        centralProcessingUnit.registerExecutor(arithmeticLogicUnit);
        centralProcessingUnit.registerExecutor(instructionProcessingUnit);
        centralProcessingUnit.registerExecutor(memoryManagementUnit);
        centralProcessingUnit.registerLocator(memoryManagementUnit);
        centralProcessingUnit.registerClockListener(memoryManagementUnit);
        centralProcessingUnit.registerClockListener(arithmeticLogicUnit);

        memoryManagementUnit.registerExecutor(centralProcessingUnit);
        memoryManagementUnit.registerLocator(inputOutputManagementUnit, (char) 0, ramAddress);
        memoryManagementUnit.registerClockListener(inputOutputManagementUnit);
        memoryManagementUnit.registerHardwareUnit(randomAccessMemory, ramAddress, ramSize);

        arithmeticLogicUnit.registerLocator(centralProcessingUnit);

        instructionProcessingUnit.registerExecutor(centralProcessingUnit);
        instructionProcessingUnit.registerLocator(centralProcessingUnit);
        instructionProcessingUnit.registerClockListener(centralProcessingUnit);
        instructionProcessingUnit.subscribe(centralProcessingUnit);

        var keyboardAddress = configParameters.getOrDefault(KEYBOARD_ADDRESS_KEY, DEFAULT_KEYBOARD_ADDRESS);
        inputOutputManagementUnit.registerHardwareUnit(keyboard, keyboardAddress, KEYBOARD_SIZE);
        inputOutputManagementUnit.registerHardwareUnit(display, displayAddress, displaySize);

        cpuClock = new Thread(() -> {
            while (running.get()) {
                synchronized (lock) {
                    if (state == State.STOPPED) {
                        try {
                            lock.wait();
                            state = State.RUNNING;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                step();
            }
        });
        cpuClock.start();
    }

    public void setInstructions(List<Instruction> instructions) {
        instructionProcessingUnit.setInstructions(instructions);
        centralProcessingUnit.getProgramCounter().setValue((char) 0);
    }

    public void addUpdateListener(Runnable listener) {
        onUpdateListeners.add(listener);
    }

    public void step() {
        synchronized (lock) {
            if (state == State.RUNNING) {
                return;
            }
        }

        instructionProcessingUnit.onTick();
        onUpdateListeners.forEach(Runnable::run);
    }

    public void run() {
        synchronized (lock) {
            if (state == State.RUNNING) {
                return;
            }

            lock.notifyAll();
        }

    }

    public void _break() {
        synchronized (lock) {
            if (state == State.STOPPED) {
                return;
            }

            state = State.STOPPED;
        }
    }

    public void terminate() throws InterruptedException {
        this.running.set(false);
        synchronized (lock) {
            lock.notifyAll();
        }
        cpuClock.join();
    }
}
