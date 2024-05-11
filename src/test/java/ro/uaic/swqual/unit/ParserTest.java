package ro.uaic.swqual.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.Parser;
import ro.uaic.swqual.exception.parser.ParserException;
import ro.uaic.swqual.exception.parser.UndefinedReferenceException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.exception.parser.DuplicateJumpTargetException;
import ro.uaic.swqual.exception.parser.JumpLabelNotFoundException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.RegisterReference;
import ro.uaic.swqual.model.operands.RelativeMemoryLocation;
import ro.uaic.swqual.proc.CentralProcessingUnit;

import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {

    private Parser parser;

    @BeforeEach
    public void setUp() {
        parser = new Parser();
    }

    @Test
    void testParseInstructionLineShouldSucceed() {
        var line = "add r0 #7;";
        parser.parseInstruction(1, line);
        var instruction = parser.getInstructions().getFirst();

        Assertions.assertEquals(InstructionType.ALU_ADD, instruction.getType());
        Assertions.assertNotNull(instruction.getParameters().getFirst());
        Assertions.assertNotNull(instruction.getParameters().getSecond());
        Assertions.assertEquals(7, instruction.getParam2().getValue());
        Assertions.assertInstanceOf(RegisterReference.class, instruction.getParam1());
        Assertions.assertEquals("r0", ((RegisterReference) instruction.getParam1()).getName());
    }

    @Test
    void testParseInputFileShouldSucceed() {
        var path = "src/test/resources/unit/test-parser.txt";
        var instructionList = parser.parse(path).getInstructions();

        Assertions.assertEquals( 4, instructionList.size());
    }

    @Test
    void testParseInputFileShouldThrowException() {
        var path = "src/test/resources/unit/test-parser-failure.txt";
        assertThrows(RuntimeException.class, () -> parser.parse(path));
    }

    @Test
    void testLinkJumpsShouldSucceed() {
        var path = "src/test/resources/unit/test-jmp.txt";
        var instructionList = parser.parse(path).getInstructions();
        parser.link();
        Assertions.assertEquals(5, instructionList.get(2).getParam1().getValue());
        Assertions.assertEquals(0, instructionList.get(7).getParam1().getValue());
    }

    @Test
    void testLinkJumpsShouldThrowNotFoundException() {
        var path = "src/test/resources/unit/test-jmp-failure.txt";
        parser.parse(path);
        assertThrows(JumpLabelNotFoundException.class, () -> parser.link());
    }

    @Test
    void testParseShouldThrowDuplicateException() {
        var path = "src/test/resources/unit/test-jmp-failure-dup.txt";
        assertThrows(DuplicateJumpTargetException.class ,() -> parser.parse(path));
    }

    @Test
    void testParseResolveReferencesShouldSucceed() {
        var code = Map.of(
                1, "add r0 r1;",
                2, "sub r3 r4;",
                3, "cmp r7 #1;"
        );

        var parser = new Parser();
        code.forEach(parser::parseInstruction);
        var instructions = parser.getInstructions();
        var cpu = new CentralProcessingUnit();

        Predicate<Instruction> containsUnresolvedReferences =
                (i) -> i.getParam1() instanceof RegisterReference || i.getParam2() instanceof RegisterReference;
        Predicate<Instruction> containsResolvedReferences =
                (i) -> i.getParam1() instanceof Register || i.getParam2() instanceof Register;

        Assertions.assertEquals(3, instructions.size());

        Assertions.assertTrue(instructions.stream().allMatch(containsUnresolvedReferences));
        Assertions.assertTrue(instructions.stream().noneMatch(containsResolvedReferences));

        parser.resolveReferences(cpu.getRegistryReferenceMap());
        Assertions.assertEquals(3, instructions.size());

        Assertions.assertTrue(instructions.stream().noneMatch(containsUnresolvedReferences));
        Assertions.assertTrue(instructions.stream().allMatch(containsResolvedReferences));
    }

    @Test
    void testParseResolveFirstReferenceShouldFail() {
        var code = Map.of(
                1, "@Label:",
                2, "add r0 r1;",
                3, "jmp @Label;",
                4, "sub r11 r4;"
        );

        var parser = new Parser();
        code.forEach(parser::parseInstruction);
        var cpu = new CentralProcessingUnit();

        assertThrows(
                UndefinedReferenceException.class,
                () -> parser.resolveReferences(cpu.getRegistryReferenceMap()),
                "Error at line 4: Undefined Reference to symbol 'r11'"
        );
    }

    @Test
    void testParseResolveSecondReferenceShouldFail() {
        var code = Map.of(
                1, "@Label:",
                2, "add r0 r1;",
                3, "jmp @Label;",
                4, "sub r4 r16;"
        );

        var parser = new Parser();
        code.forEach(parser::parseInstruction);
        var cpu = new CentralProcessingUnit();

        assertThrows(
                UndefinedReferenceException.class,
                () -> parser.resolveReferences(cpu.getRegistryReferenceMap()),
                "Error at line 4: Undefined Reference to symbol 'r16'"
        );
    }

    @Test
    void parseLineWithoutTerminationShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add r0 r1"));
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "@l0"));
    }

    @Test
    void parseRelativeMemLocShouldReturnValidAbsMemLoc() {
        var parser = new Parser();
        var cpu = new CentralProcessingUnit();
        var regRef = cpu.getRegistryReferenceMap();
        var r1 = cpu.getDataRegisters().get(1);
        var r3 = cpu.getDataRegisters().get(3);
        var p0 = parser.parseInstruction(0, "add [r1 + 50 - r3] r0;")
                .resolveReferences(regRef).getInstructions().getFirst().getParam1();
        assertInstanceOf(RelativeMemoryLocation.class, p0);
        assertEquals((char) 50, p0.getValue());
        r3.setValue((char) 25);
        assertEquals((char) 25, p0.getValue());
        r1.setValue((char) 100);
        assertEquals((char) 125, p0.getValue());
    }

    @Test
    void parseMemLocUnterminatedLocShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add [r1 r0;"));
    }

    @Test
    void parseMemLocDuplicateLocShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add [[r1] r0;"));
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add [ [r1] r0;"));
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add [ [r1 ] r0;"));
    }

    @Test
    void parseMemLocTermWithoutOpenLocShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add r1] r0;"));
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add r1 ] r0;"));
    }
}