package ro.uaic.swqual;

import ro.uaic.swqual.exception.InstructionError;
import ro.uaic.swqual.exception.ParameterError;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static ro.uaic.swqual.InstructionType.*;

public class Processor {
    private final List<Register> dataRegisters = new ArrayList<>();

    private final Register specialReg0 = new Register();
    private final FlagRegister flagRegister = new FlagRegister();
    private final ALU alu = new ALU(flagRegister, specialReg0);

    public Processor() {
        IntStream.range(0, 8).forEach(regIndex -> dataRegisters.add(new Register()));
    }

    public static boolean isInRange(
            InstructionType inQuestion,
            InstructionType lowerInclusive,
            InstructionType upperExclusive
    ) {
        return inQuestion.ordinal() >= lowerInclusive.ordinal()
            && inQuestion.ordinal() < upperExclusive.ordinal();
    }

    public void execute(Instruction instruction) throws InstructionError, ParameterError {
        if (isInRange(instruction.getType(), ALU_ADD, ALU_CMP)) {
            alu.execute(instruction);
        }

        throw new InstructionError("Unhandled instruction '" + instruction.getType() + "'");
    }

    public List<Register> getDataRegisters() {
        return dataRegisters;
    }
}
