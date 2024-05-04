package ro.uaic.swqual;

import ro.uaic.swqual.exception.InstructionError;
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

    public void execute(InstructionType instruction, Register destSource1, Register source2) throws InstructionError {
        if (isInRange(instruction, ALU_ADD, ALU_CMP)) {
            alu.execute(instruction, destSource1, source2);
        }

        throw new InstructionError("Unhandled instruction '" + instruction + "'");
    }

    public List<Register> getDataRegisters() {
        return dataRegisters;
    }
}
