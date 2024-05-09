package ro.uaic.swqual.unit.model;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.util.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InstructionTest {
    @Test
    void defaultConstructInstructionShouldHaveNoValues() {
        var instr = new Instruction();
        assertNull(instr.getType());
        assertNull(instr.getParam1());
        assertNull(instr.getParam2());
        assertNull(instr.getParameters().getFirst());
        assertNull(instr.getParameters().getSecond());
    }

    @Test
    void instructionTypeConstructorShouldConstructObjectWithNoParameters() {
        var instr = new Instruction(InstructionType.MMU_MOV);
        assertEquals(InstructionType.MMU_MOV, instr.getType());
        assertNull(instr.getParam1());
        assertNull(instr.getParam2());
        assertNull(instr.getParameters().getFirst());
        assertNull(instr.getParameters().getSecond());
    }

    @Test
    void oneParamConstructorShouldConstructObjectWithOneParameter() {
        var instr = new Instruction(InstructionType.MMU_MOV, new Constant((char) 10));
        assertEquals(InstructionType.MMU_MOV, instr.getType());
        assertNotNull(instr.getParam1());
        assertEquals(10, instr.getParam1().getValue());
        assertNull(instr.getParam2());
        assertEquals(instr.getParam1(), instr.getParameters().getFirst());
        assertNull(instr.getParameters().getSecond());
    }

    @Test
    void twoParamConstructorShouldConstructObjectWithTwoParameters() {
        var instr = new Instruction(InstructionType.MMU_MOV, new Constant((char) 10), new Constant((char) 50));
        assertEquals(InstructionType.MMU_MOV, instr.getType());
        assertNotNull(instr.getParam1());
        assertEquals(10, instr.getParam1().getValue());
        assertNotNull(instr.getParam2());
        assertEquals(50, instr.getParam2().getValue());
        assertEquals(instr.getParam1(), instr.getParameters().getFirst());
        assertEquals(instr.getParam2(), instr.getParameters().getSecond());
    }

    @Test
    void instructionTypeSetterShouldUpdate() {
        var instr = new Instruction(InstructionType.MMU_MOV);
        assertEquals(InstructionType.MMU_MOV, instr.getType());
        instr.setType(InstructionType.MMU_PUSH);
        assertEquals(InstructionType.MMU_PUSH, instr.getType());
    }

    @Test
    void instructionParam1SetterShouldUpdate() {
        var instr = new Instruction(null, null);
        assertNull(instr.getParam1());
        instr.setParam1(new Constant((char) 20));
        assertNotNull(instr.getParam1());
        assertEquals(20, instr.getParam1().getValue());
    }

    @Test
    void instructionParam2SetterShouldUpdate() {
        var instr = new Instruction(null, null, null);
        assertNull(instr.getParam2());
        instr.setParam2(new Constant((char) 20));
        assertNotNull(instr.getParam2());
        assertEquals(20, instr.getParam2().getValue());
    }

    @Test
    void instructionParameterSetShouldUpdate() {
        var instr = new Instruction(null, null, null);

        assertNull(instr.getParameters().getFirst());
        assertNull(instr.getParameters().getSecond());

        instr.setParameters(Tuple.of(new Constant((char) 10), null));
        assertNotNull(instr.getParameters().getFirst());
        assertEquals(10, instr.getParameters().getFirst().getValue());
        assertNull(instr.getParameters().getSecond());

        instr.setParameters(Tuple.of(null, new Constant((char) 20)));
        assertNull(instr.getParameters().getFirst());
        assertNotNull(instr.getParameters().getSecond());
        assertEquals(20, instr.getParameters().getSecond().getValue());

        instr.setParameters(Tuple.of(new Constant((char) 10), new Constant((char) 20)));
        assertNotNull(instr.getParameters().getFirst());
        assertEquals(10, instr.getParameters().getFirst().getValue());
        assertNotNull(instr.getParameters().getSecond());
        assertEquals(20, instr.getParameters().getSecond().getValue());

        instr.setParameters(Tuple.of(null, null));
        assertNull(instr.getParameters().getFirst());
        assertNull(instr.getParameters().getSecond());
    }
}
