package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Instruction Processing Unit
 * Its purpose is to inform the units awaiting instructions of the next instruction, to advance the attached
 *      program counter, and to modify it on demand.
 */
public class InstructionProcessingUnit extends DelegatingUnit {
    private final FlagRegister flagRegister;
    private final Register programCounter;
    private final List<Instruction> instructions;
    private final List<ProcessingUnit> instructionSubscribers = new ArrayList<>();
    public static final Instruction defaultInstruction = new Instruction(InstructionType.IPU_JMP, new Constant((char)0));

    public InstructionProcessingUnit(List<Instruction> instructions, FlagRegister flagRegister, Register programCounter) {
        this.flagRegister = flagRegister;
        this.programCounter = programCounter;
        this.instructions = instructions;
    }

    @Override
    public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    public void subscribe(ProcessingUnit processingUnit) {
        instructionSubscribers.add(processingUnit);
    }

    private boolean isJumpApproved(InstructionType instructionType) {
        return switch (instructionType) {
            case IPU_JMP -> true;
            case IPU_JEQ -> flagRegister.isSet(FlagRegister.EQUAL_FLAG);
            case IPU_JNE -> !flagRegister.isSet(FlagRegister.EQUAL_FLAG);
            case IPU_JLT -> flagRegister.isSet(FlagRegister.LESS_FLAG) && !flagRegister.isSet(FlagRegister.EQUAL_FLAG);
            case IPU_JLE -> flagRegister.isSet(FlagRegister.LESS_FLAG) || flagRegister.isSet(FlagRegister.EQUAL_FLAG);
            case IPU_JGT -> !flagRegister.isSet(FlagRegister.LESS_FLAG) && !flagRegister.isSet(FlagRegister.EQUAL_FLAG);
            case IPU_JGE -> !flagRegister.isSet(FlagRegister.LESS_FLAG) || flagRegister.isSet(FlagRegister.EQUAL_FLAG);
            default -> throw new InstructionException("Unknown instruction type: '" + instructionType + "'");
        };
    }

    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> instruction.getType().ordinal() >= InstructionType.IPU_JMP.ordinal()
                           && instruction.getType().ordinal() <= InstructionType.IPU_JGE.ordinal();
    }

    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        if (isJumpApproved(instruction.getType())) {
            programCounter.setValue((char)(instruction.getParam1().getValue() - 1));
        }
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
    }

    public Instruction next() {
        if (programCounter.getValue() >= instructions.size()) {
            return defaultInstruction;
        }
        return instructions.get(programCounter.getValue());
    }
}
