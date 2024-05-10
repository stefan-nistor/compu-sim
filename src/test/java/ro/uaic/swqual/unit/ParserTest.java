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
import ro.uaic.swqual.proc.CentralProcessingUnit;

import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void parseValidBase2ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add 0b011 0B10001;").getInstructions();
        assertEquals((char)0b011, instr.getFirst().getParam1().getValue());
        assertEquals((char)0b10001, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parseInvalidBase2ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add 0b 0B;"));
    }

    @Test
    void parsePrefixedValidBase2ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add #0b011 #0B10001;").getInstructions();
        assertEquals((char)0b011, instr.getFirst().getParam1().getValue());
        assertEquals((char)0b10001, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parsePrefixedInvalidBase2ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add #0b #0B;"));
    }

    @Test
    void parseValidBase8ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add 071 052;").getInstructions();
        assertEquals((char) 57, instr.getFirst().getParam1().getValue());
        assertEquals((char) 42, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parseInvalidBase8ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add 08 09;"));
    }

    @Test
    void parsePrefixedValidBase8ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add #071 #052;").getInstructions();
        assertEquals((char) 57, instr.getFirst().getParam1().getValue());
        assertEquals((char) 42, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parsePrefixedInvalidBase8ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add #08 #09;"));
    }

    @Test
    void parseValidBase10ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add 71 52;").getInstructions();
        assertEquals((char) 71, instr.getFirst().getParam1().getValue());
        assertEquals((char) 52, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parseInvalidBase10ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add 1a 1b;"));
    }

    @Test
    void parsePrefixedValidBase10ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add #71 #52;").getInstructions();
        assertEquals((char) 71, instr.getFirst().getParam1().getValue());
        assertEquals((char) 52, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parsePrefixedInvalidBase10ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add #1a #1b;"));
    }

    @Test
    void parseValidBase16ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add 0x71af 0X52EF;").getInstructions();
        assertEquals((char) 0x71af, instr.getFirst().getParam1().getValue());
        assertEquals((char) 0x52ef, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parseInvalidBase16ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add 0xay 0xaz;"));
    }

    @Test
    void parsePrefixedValidBase16ConstantsShouldSucceed() {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add #0x71af #0X52EF;").getInstructions();
        assertEquals((char) 0x71af, instr.getFirst().getParam1().getValue());
        assertEquals((char) 0x52ef, instr.getFirst().getParam2().getValue());
    }

    @Test
    void parsePrefixedInvalidBase16ConstantsShouldThrow() {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add #0xay #0xaz;"));
    }
}