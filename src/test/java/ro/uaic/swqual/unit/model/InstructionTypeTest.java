package ro.uaic.swqual.unit.model;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.InstructionType;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ro.uaic.swqual.model.InstructionType.ALU_ADD;
import static ro.uaic.swqual.model.InstructionType.ALU_AND;
import static ro.uaic.swqual.model.InstructionType.ALU_CMP;
import static ro.uaic.swqual.model.InstructionType.ALU_NOT;
import static ro.uaic.swqual.model.InstructionType.ALU_OR;
import static ro.uaic.swqual.model.InstructionType.ALU_SDIV;
import static ro.uaic.swqual.model.InstructionType.ALU_SHL;
import static ro.uaic.swqual.model.InstructionType.ALU_SHR;
import static ro.uaic.swqual.model.InstructionType.ALU_SMUL;
import static ro.uaic.swqual.model.InstructionType.ALU_SUB;
import static ro.uaic.swqual.model.InstructionType.ALU_UDIV;
import static ro.uaic.swqual.model.InstructionType.ALU_UMUL;
import static ro.uaic.swqual.model.InstructionType.ALU_XOR;
import static ro.uaic.swqual.model.InstructionType.IPU_CALL;
import static ro.uaic.swqual.model.InstructionType.IPU_JEQ;
import static ro.uaic.swqual.model.InstructionType.IPU_JGE;
import static ro.uaic.swqual.model.InstructionType.IPU_JGT;
import static ro.uaic.swqual.model.InstructionType.IPU_JLE;
import static ro.uaic.swqual.model.InstructionType.IPU_JLT;
import static ro.uaic.swqual.model.InstructionType.IPU_JMP;
import static ro.uaic.swqual.model.InstructionType.IPU_JNE;
import static ro.uaic.swqual.model.InstructionType.IPU_RET;
import static ro.uaic.swqual.model.InstructionType.MMU_MOV;
import static ro.uaic.swqual.model.InstructionType.MMU_POP;
import static ro.uaic.swqual.model.InstructionType.MMU_PUSH;
import static ro.uaic.swqual.model.InstructionType.values;
import static ro.uaic.swqual.model.InstructionType.fromLabel;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionTypeTest {
    @Test
    void fromLabelShouldCoversAllCases() {
        assertTrue(stream(values()).allMatch(value -> fromLabel(value.label) == value));
    }

    @Test
    void isMmuInstructionShouldOnlyAcceptMmuInstructions() {
        assertEquals(
                Arrays.stream(InstructionType.values())
                        .filter(InstructionType::isMmuInstruction)
                        .toList(),
                List.of(MMU_MOV, MMU_PUSH, MMU_POP)
        );
    }

    @Test
    void isAluInstructionShouldOnlyAcceptAluInstructions() {
        assertEquals(
                Arrays.stream(InstructionType.values())
                        .filter(InstructionType::isAluInstruction)
                        .toList(),
                List.of(
                        ALU_ADD, ALU_SUB, ALU_UMUL, ALU_SMUL, ALU_UDIV, ALU_SDIV,
                        ALU_AND, ALU_OR, ALU_XOR, ALU_SHL, ALU_SHR, ALU_NOT, ALU_CMP
                )
        );
    }

    @Test
    void isIpuInstructionShouldOnlyAcceptIpuInstructions() {
        assertEquals(
                Arrays.stream(InstructionType.values())
                        .filter(InstructionType::isIpuInstruction)
                        .toList(),
                List.of(
                        IPU_JMP, IPU_JEQ, IPU_JNE, IPU_JLT, IPU_JLE,
                        IPU_JGT, IPU_JGE, IPU_CALL, IPU_RET
                )
        );
    }
}
