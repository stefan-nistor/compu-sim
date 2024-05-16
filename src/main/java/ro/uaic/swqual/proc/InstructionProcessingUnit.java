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
import ro.uaic.swqual.model.operands.UnresolvedMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static ro.uaic.swqual.model.InstructionType.IPU_JMP;
import static ro.uaic.swqual.model.InstructionType.MMU_POP;
import static ro.uaic.swqual.model.InstructionType.MMU_PUSH;
import static ro.uaic.swqual.model.operands.FlagRegister.DIV_ZERO_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.EQUAL_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.LESS_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.OVERFLOW_FLAG;

/**
 * Instruction Processing Unit
 * Its purpose is to inform the units awaiting instructions of the next instruction, to advance the attached
 *      program counter, and to modify it on demand.
 */
public class InstructionProcessingUnit extends DelegatingUnit implements ClockListener {
    private final FlagRegister flagRegister;
    private final Register programCounter;
    private final List<Instruction> instructions;
    private final List<ProcessingUnit> instructionSubscribers = new ArrayList<>();
    private final Instruction pushCallLoc;
    private final Instruction pop;
    private final AbsoluteMemoryLocation stackHeadReference;
    public static final Instruction defaultInstruction = new Instruction(IPU_JMP, new Constant((char)0));

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
        pushCallLoc = new Instruction(MMU_PUSH);
        pop = new Instruction(MMU_POP);
        stackHeadReference = new AbsoluteMemoryLocation(stackPointer);
    }

    @Override
    public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    public void subscribe(ProcessingUnit processingUnit) {
        registerPotentialClockListener(processingUnit);
        instructionSubscribers.add(processingUnit);
    }

    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> InstructionType.isIpuInstruction(instruction.getType());
    }

    private void jump(Parameter at) {
        assert at != null;
        programCounter.setValue((char)(at.getValue() - 1));
    }

    private void conditionedJump(boolean condition, Parameter at) {
        if (condition) {
            jump(at);
        }
    }

    private void ret() {
        super.execute(pop);
        // We also need to increment the PC by 1, since the retained PC is, in fact, the PC of the call instruction.
        // We want to go after the call point.
        programCounter.setValue(locate(stackHeadReference).getValue());
    }

    private void call(Parameter address) {
        pushCallLoc.setParam1(programCounter);
        super.execute(pushCallLoc);
        jump(address);
    }

    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        assert instruction != null;
        var type = instruction.getType();
        var p0 = locate(instruction.getParam1());
        assert !(p0 instanceof ResolvedMemory);
        Consumer<Boolean> conditionedJumpAtP0 = condition -> conditionedJump(condition, p0);
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

    public void setInstructions(List<Instruction> instructions) {
        assert instructions != null;
        this.instructions.clear();
        this.instructions.addAll(instructions);
    }

    public void reset() {
        programCounter.setValue((char) 0);
    }

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

    public Instruction next() {
        if (programCounter.getValue() >= instructions.size()) {
            return defaultInstruction;
        }
        var instruction = instructions.get(programCounter.getValue());
        assert instruction != null;
        return instruction;
    }
}
