package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;

import java.util.function.Predicate;

/**
 * Represents the minimal interface for a Processing Unit. Allows filtering, executing instructions, and raising errors.
 */
public interface ProcessingUnit {
    /**
     * Method used to execute a given instruction.
     * @param instruction instruction to execute.
     * @throws InstructionException when given instruction cannot or should not be processed by
     *   the current {@link ProcessingUnit}
     * @throws ParameterException when given instruction contains any invalid/incompatible
     *   {@link ro.uaic.swqual.model.operands.Parameter Parameter} values, such as
     *   {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}
     */
    void execute(Instruction instruction) throws InstructionException, ParameterException;

    /**
     * Method used to raise an error via a flag value, present in
     *   {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister}.
     * @param value flag value to raise.
     */
    void raiseFlag(char value);

    /**
     * Method used to acquire a filtering interface for instructions. Validates whether the current
     *   unit can execute an {@link Instruction}. Will accept all unless overridden.
     * @return the functional interface used for validation.
     */
    default Predicate<Instruction> getDefaultFilter() {
        return instruction -> true;
    }
}
