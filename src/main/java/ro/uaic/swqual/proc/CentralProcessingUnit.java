package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Represents the central unit routing information to and from other registered {@link ProcessingUnit ProcessingUnits}.
 * All {@link ProcessingUnit#execute}, {@link LocatingUnit#locate} and {@link ClockListener#onTick} is expected to pass
 * through this object first in a fully functional model.
 */
public class CentralProcessingUnit extends DelegatingUnit {
    /** List containing processor registers */
    private final List<Register> dataRegisters = new ArrayList<>();

    /* Special purpose registers */
    /** {@link FlagRegister} holding status and error flags */
    private final FlagRegister flagRegister = new FlagRegister();
    /** Program counter value, representing next address to be executed */
    private final Register programCounter = new Register();
    /** Stack pointer, holding the address of the top of the stack */
    private final Register stackPointer = new Register();

    /** Map providing association from assembly code registry name to actual {@link Register} instances */
    private final Map<String, Register> registryReferenceMap = new HashMap<>();

    /**
     * Method used to reset {@link CentralProcessingUnit#flagRegister} before executing an instruction.
     * Will skip this step if next instruction
     *   depends on the flag value.
     * @param instructionType type of the next instruction
     */
    private void prepareStateBefore(InstructionType instructionType) {
        assert instructionType != null;
        // IPU instructions require the current flag state.
        if (!InstructionType.isIpuInstruction(instructionType)) {
            flagRegister.clear();
        }
    }

    /**
     * Primary and default constructor.
     * Initializes the registry reference map to the {@link Register} instances present in the current object.
     */
    public CentralProcessingUnit() {
        IntStream.range(0, 8).forEach(regIndex -> {
            dataRegisters.add(new Register());
            registryReferenceMap.put("r" + regIndex, dataRegisters.getLast());
        });

        registryReferenceMap.put("sp", stackPointer);
        registryReferenceMap.put("pc", programCounter);
    }

    /**
     * Method used to execute a given instruction.
     * Will clear the flags and pass the execution to the delegated units using {@link DelegatingUnit#execute}.
     * @param instruction instruction to execute.
     * @throws InstructionException when given instruction cannot or should not be processed by
     *   the current {@link ProcessingUnit} or any delegated units.
     * @throws ParameterException when given instruction contains any invalid/incompatible
     *   {@link ro.uaic.swqual.model.operands.Parameter Parameter} values, such as
     *   {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}
     */
    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        assert instruction != null;
        prepareStateBefore(instruction.getType());
        super.execute(instruction);
    }

    /**
     * Method used to raise an error directly to {@link CentralProcessingUnit#flagRegister}.
     * @param value flag value to raise.
     */
    @Override public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    /**
     * Getter for {@link CentralProcessingUnit#flagRegister}
     * @return reference to the register
     */
    public FlagRegister getFlagRegister() {
        return flagRegister;
    }

    /**
     * Getter for {@link CentralProcessingUnit#programCounter}
     * @return reference to the register
     */
    public Register getProgramCounter() {
        return programCounter;
    }

    /**
     * Getter for {@link CentralProcessingUnit#stackPointer}
     * @return reference to the register
     */
    public Register getStackPointer() {
        return stackPointer;
    }

    /**
     * Getter for {@link CentralProcessingUnit#dataRegisters}
     * @return reference to the list of data registers
     */
    public List<Register> getDataRegisters() {
        return dataRegisters;
    }

    /**
     * Getter for {@link CentralProcessingUnit#registryReferenceMap}
     * @return reference to the register reference map
     */
    public Map<String, Register> getRegistryReferenceMap() {
        return registryReferenceMap;
    }
}
