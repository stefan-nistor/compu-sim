package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.ResolvedMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static ro.uaic.swqual.model.InstructionType.IPU_JMP;
import static ro.uaic.swqual.model.InstructionType.MMU_POP;
import static ro.uaic.swqual.model.InstructionType.MMU_PUSH;
import static ro.uaic.swqual.model.operands.FlagRegister.EQUAL_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.LESS_FLAG;

/**
 * Represents the program executor informer. <br/>
 * On each tick, it will acquire the next instruction and pass it to the
 * registered subscribers. <br/>
 * <br/>
 * In a standard scenario, this would be only the {@link CentralProcessingUnit}, as it will
 * pass those to the linked {@link ProcessingUnit ProcessingUnits}.
 */
public class InstructionProcessingUnit extends DelegatingUnit implements ClockListener {
    /** Reference to the {@link FlagRegister} to raise errors to */
    private final FlagRegister flagRegister;
    /** Reference to the program counter {@link Register} to use when identifying instruction index. */
    private final Register programCounter;
    /** List of {@link Instruction Instructions} present in the currently executed program */
    private final List<Instruction> instructions;
    /** List of {@link ProcessingUnit ProcessingUnits} acting as the
      * currently executed {@link Instruction} entry point. */
    private final List<ProcessingUnit> instructionSubscribers = new ArrayList<>();
    /** Special prebuilt {@link Instruction}.
     *  Acts as the {@link InstructionType#MMU_PUSH push} before {@link InstructionType#IPU_CALL call} */
    private final Instruction pushCallLoc;
    /** Special prebuilt {@link Instruction}.
     *  Acts as the {@link InstructionType#MMU_POP pop} before {@link InstructionType#IPU_RET ret} */
    private final Instruction pop;
    /** {@link AbsoluteMemoryLocation} built on top of the Stack Pointer {@link Register}. Represents
      * the address of the stack head
      * (whereas the {@link CentralProcessingUnit#getStackPointer stackPointer} register
      * is the value of that address). */
    private final AbsoluteMemoryLocation stackHeadReference;
    /** Default {@link Instruction} to be used when there are no more instructions in the
      * {@link InstructionProcessingUnit#instructions list} to be run. Effectively resets the program. */
    public static final Instruction defaultInstruction = new Instruction(IPU_JMP, new Constant((char)0));

    /**
     * Primary constructor
     * @param instructions the list of instructions to run on each clock cycle
     * @param flagRegister reference to the {@link FlagRegister} to be used for raising status and errors
     * @param programCounter reference to the {@link Register} that acts as the program counter
     * @param stackPointer reference to the {@link Register} that acts as the stack pointer
     */
    public InstructionProcessingUnit(
            List<Instruction> instructions,
            FlagRegister flagRegister,
            Register programCounter,
            Register stackPointer
    ) {
        assert instructions != null;
        assert flagRegister != null;
        assert programCounter != null;
        assert stackPointer != null;
        this.flagRegister = flagRegister;
        this.programCounter = programCounter;
        this.instructions = instructions;
        // pre-construct the relevant instructions and location of the stack head.
        pushCallLoc = new Instruction(MMU_PUSH);
        pop = new Instruction(MMU_POP);
        stackHeadReference = new AbsoluteMemoryLocation(stackPointer);
    }

    /**
     * Method used to raise an error via a flag value, present in
     *   {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister}.
     * @param value flag value to raise.
     */
    @Override
    public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    /**
     * Method used to add a {@link ProcessingUnit} as the entry point executor for the program's instructions.
     * @param processingUnit unit to add to the executors.
     */
    public void subscribe(ProcessingUnit processingUnit) {
        registerPotentialClockListener(processingUnit);
        instructionSubscribers.add(processingUnit);
    }

    /**
     * Default filter for instructions. Accepts instructions according to {@link InstructionType#isIpuInstruction}.
     * @return The filter interface in question.
     */
    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> InstructionType.isIpuInstruction(instruction.getType());
    }

    /**
     * Method executing the {@link InstructionType#IPU_JMP jmp} instruction. Is also called by conditioned jumps,
     * such as {@link InstructionType#IPU_JEQ jeq} after validating the condition.
     * @param at address to jump to
     */
    private void jump(Parameter at) {
        assert at != null;
        programCounter.setValue((char)(at.getValue() - 1));
    }

    /**
     * Method used to act as the conditioned jump (such as {@link InstructionType#IPU_JEQ jeq}) instruction entry point.
     * @param condition condition to validate before jumping
     * @param at address to jump to if condition is valid
     */
    private void conditionedJump(boolean condition, Parameter at) {
        if (condition) {
            jump(at);
        }
    }

    /**
     * Method executing the {@link InstructionType#IPU_RET ret} instruction. <br/>
     * It will request an execution of the prebuilt {@link InstructionProcessingUnit#pop} instruction through
     * the registered delegators. After this, it will update the
     *   {@link InstructionProcessingUnit#programCounter} with what was stored on the stack as the return address. <br/>
     * In a default scenario, the pop prebuilt pop will route through {@link CentralProcessingUnit}.
     */
    private void ret() {
        super.execute(pop);
        // We also need to increment the PC by 1, since the retained PC is, in fact, the PC of the call instruction.
        // We want to go after the call point.
        programCounter.setValue(locate(stackHeadReference).getValue());
    }

    /**
     * Method executing the {@link InstructionType#IPU_CALL call} instruction. <br/>
     * It will push the current {@link InstructionProcessingUnit#programCounter} value onto the stack
     *   using the prebuilt {@link InstructionProcessingUnit#pushCallLoc} instruction through the registered delegators.
     *   After this, it will execute the jump to the requested location (function address - label). <br/>
     * In a default scenario, the pop prebuilt pop will route through {@link CentralProcessingUnit}.
     * @param address the location of the function to call
     */
    private void call(Parameter address) {
        pushCallLoc.setParam1(programCounter);
        super.execute(pushCallLoc);
        jump(address);
    }

    /**
     * Method used to execute a given instruction.
     * @param instruction instruction to execute.
     * @throws InstructionException when given instruction cannot or should not be processed by
     *   the current {@link ProcessingUnit}
     * @throws ParameterException when given instruction contains any invalid/incompatible
     *   {@link ro.uaic.swqual.model.operands.Parameter Parameter} values, such as
     *   {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}
     */
    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        assert instruction != null;
        var type = instruction.getType();
        // ensure that MemoryLocation parameters resolve to values (ResolvedMemory)
        var p0 = locate(instruction.getParam1());
        assert !(p0 instanceof ResolvedMemory);

        // This acts as the conditional jump wrapper.
        Consumer<Boolean> conditionedJumpAtP0 = condition -> conditionedJump(condition, p0);
        // route parameter to requested instruction.
        // Note: the conditional jumps follow the same logic presented in ArithmeticLogicUnit::compare
        switch (type) {
            case IPU_JMP -> jump(p0);
            case IPU_JEQ -> conditionedJumpAtP0.accept(flagRegister.isSet(EQUAL_FLAG));
            case IPU_JNE -> conditionedJumpAtP0.accept(!flagRegister.isSet(EQUAL_FLAG));
            case IPU_JLT -> conditionedJumpAtP0.accept(flagRegister.isSet(LESS_FLAG) && !flagRegister.isSet(EQUAL_FLAG));
            case IPU_JLE -> conditionedJumpAtP0.accept(flagRegister.isSet(LESS_FLAG) || flagRegister.isSet(EQUAL_FLAG));
            case IPU_JGT -> conditionedJumpAtP0.accept(!flagRegister.isSet(LESS_FLAG) && !flagRegister.isSet(EQUAL_FLAG));
            case IPU_JGE -> conditionedJumpAtP0.accept(!flagRegister.isSet(LESS_FLAG) || flagRegister.isSet(EQUAL_FLAG));
            case IPU_RET -> ret();
            case IPU_CALL -> call(p0);
            default -> throw new InstructionException("Unknown instruction type: " + type);
        }
    }

    /**
     * Instruction List setter. Updates the currently executed instruction list.
     * @param instructions the new instruction list
     */
    public void setInstructions(List<Instruction> instructions) {
        assert instructions != null;
        this.instructions.clear();
        this.instructions.addAll(instructions);
    }

    /**
     * Method used to reset the program. It will effectively set {@link InstructionProcessingUnit#programCounter} to 0.
     */
    public void reset() {
        programCounter.setValue((char) 0);
    }

    /**
     * Method to be called on each clock tick. <br/>
     * It will fetch the next instruction and pass it to the {@link ProcessingUnit} entry points found in
     *   {@link InstructionProcessingUnit#instructionSubscribers}.
     *   After this, it will also increment the {@link InstructionProcessingUnit#programCounter} by 1. <br/>
     * <br/>
     * If no next instruction exists, the {@link InstructionProcessingUnit#defaultInstruction} is run.
     */
    @Override
    public void onTick() {
        var nextInstruction = next();
        if (nextInstruction == defaultInstruction) {
            // Executing the default instruction is equivalent to a "warm-reset"
            flagRegister.set(FlagRegister.ILLEGAL_FLAG);
        }
        instructionSubscribers.forEach(s -> s.execute(next()));
        programCounter.setValue((char)(programCounter.getValue() + 1));
        super.onTick();
    }

    /**
     * Method used to acquire the next instruction.
     * If no next instruction exists, the {@link InstructionProcessingUnit#defaultInstruction} is returned.
     * @return the next {@link Instruction} to be run.
     */
    public Instruction next() {
        if (programCounter.getValue() >= instructions.size()) {
            return defaultInstruction;
        }
        var instruction = instructions.get(programCounter.getValue());
        assert instruction != null;
        return instruction;
    }
}
