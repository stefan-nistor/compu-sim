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

public class CentralProcessingUnit extends DelegatingUnit {
    private final List<Register> dataRegisters = new ArrayList<>();

    // Special purpose registers
    private final FlagRegister flagRegister = new FlagRegister();
    private final Register programCounter = new Register();
    private final Register stackPointer = new Register();

    private final Map<String, Register> registryReferenceMap = new HashMap<>();

    private void prepareStateBefore(InstructionType instructionType) {
        if (!InstructionType.isIpuInstruction(instructionType)) {
            flagRegister.clear();
        }
    }

    public CentralProcessingUnit() {
        IntStream.range(0, 8).forEach(regIndex -> {
            dataRegisters.add(new Register());
            registryReferenceMap.put("r" + regIndex, dataRegisters.getLast());
        });

        registryReferenceMap.put("sp", stackPointer);
        registryReferenceMap.put("pc", programCounter);
    }

    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        prepareStateBefore(instruction.getType());
        super.execute(instruction);
    }

    @Override public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    public FlagRegister getFlagRegister() {
        return flagRegister;
    }

    public Register getProgramCounter() {
        return programCounter;
    }

    public Register getStackPointer() {
        return stackPointer;
    }

    public List<Register> getDataRegisters() {
        return dataRegisters;
    }

    public Map<String, Register> getRegistryReferenceMap() {
        return registryReferenceMap;
    }
}
