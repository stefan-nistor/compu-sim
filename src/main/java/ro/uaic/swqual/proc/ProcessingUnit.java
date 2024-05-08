package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;

import java.util.function.Predicate;

public interface ProcessingUnit {
    void execute(Instruction instruction) throws InstructionException, ParameterException;
    void raiseFlag(char value);
    default Predicate<Instruction> getDefaultFilter() {
        return instruction -> true;
    }
}
