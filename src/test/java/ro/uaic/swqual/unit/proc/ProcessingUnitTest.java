package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.proc.CPU;
import ro.uaic.swqual.proc.ProcessingUnit;

import java.util.List;

class ProcessingUnitTest implements ProcTestUtility {
    @Test
    void dummyProcessingUnitTest() {
        var p = new ProcessingUnit() {
            Instruction last = null;

            public Instruction getLast() {
                return last;
            }

            @Override
            public void execute(Instruction instruction) throws InstructionException, ParameterException {
                last = instruction;
            }

            @Override
            public void raiseFlag(char value) {
                discard(value);
            }
        };

        var instructions = List.of(
                new Instruction(InstructionType.ALU_ADD, null, null),
                new Instruction(InstructionType.ALU_SUB, null, null),
                new Instruction(InstructionType.ALU_CMP, null, null)
        );

        for (Instruction instruction : instructions) {
            p.execute(instruction);
            Assertions.assertEquals(p.getLast().getType(), instruction.getType());
        }
    }

    @Test
    void dummyProcessingUnitDefaultFilterTest() {
        var unit = new ProcessingUnit() {
            Instruction last = null;

            public Instruction getLast() {
                return last;
            }

            @Override
            public void execute(Instruction instruction) throws InstructionException, ParameterException {
                last = instruction;
            }

            @Override
            public void raiseFlag(char value) {
                discard(value);
            }
        };
        var proc = new CPU();
        proc.registerExecutor(unit);

        var instructions = List.of(
                new Instruction(InstructionType.ALU_ADD, null, null),
                new Instruction(InstructionType.ALU_SUB, null, null),
                new Instruction(InstructionType.ALU_CMP, null, null)
        );

        for (Instruction instruction : instructions) {
            proc.execute(instruction);
            Assertions.assertEquals(unit.getLast().getType(), instruction.getType());
        }
    }
}
