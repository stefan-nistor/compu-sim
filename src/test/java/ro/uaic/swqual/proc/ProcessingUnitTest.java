package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;

import java.util.List;

public class ProcessingUnitTest {
    @Test
    public void dummyProcessingUnitTest() {
        var p = new ProcessingUnit() {
            Instruction last = null;

            public Instruction getLast() {
                return last;
            }

            @Override
            public void execute(Instruction instruction) throws InstructionException, ParameterException {
                last = instruction;
            }
        };

        var instructions = List.of(
                new Instruction(InstructionType.ALU_ADD, null, null),
                new Instruction(InstructionType.ALU_SUB, null, null),
                new Instruction(InstructionType.ALU_CMP, null, null)
        );

        for (Instruction instruction : instructions) {
            p.execute(instruction);
            Assert.assertEquals(p.getLast().getType(), instruction.getType());
        }
    }

    @Test
    public void dummyProcessingUnitDefaultFilterTest() {
        var unit = new ProcessingUnit() {
            Instruction last = null;

            public Instruction getLast() {
                return last;
            }

            @Override
            public void execute(Instruction instruction) throws InstructionException, ParameterException {
                last = instruction;
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
            Assert.assertEquals(unit.getLast().getType(), instruction.getType());
        }
    }
}
