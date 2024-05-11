package ro.uaic.swqual.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.Parser;
import ro.uaic.swqual.exception.parser.ParserException;
import ro.uaic.swqual.exception.parser.UndefinedReferenceException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
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

    void parseValidIntValues(String vs0, String vs1, int v0, int v1) {
        var parser = new Parser();
        var instr = parser.parseInstruction(0, "add " + vs0 + " " + vs1 + ";").getInstructions();
        assertEquals((char) v0, instr.getFirst().getParam1().getValue());
        assertEquals((char) v1, instr.getFirst().getParam2().getValue());
    }

    void parseInvalidIntValues(String vs0, String vs1) {
        var parser = new Parser();
        assertThrows(ParserException.class, () -> parser.parseInstruction(0, "add " + vs0 + " " + vs1 + ";"));
    }

    @Test
    void parseValidBase2ConstantsShouldSucceed() {
        parseValidIntValues("0b011", "0B10001", 0b011, 0B10001);
    }

    @Test
    void parseInvalidBase2ConstantsShouldThrow() {
        parseInvalidIntValues("0b", "0B");
    }

    @Test
    void parsePrefixedValidBase2ConstantsShouldSucceed() {
        parseValidIntValues("#0b011", "#0B10001", 0b011, 0B10001);
    }

    @Test
    void parsePrefixedInvalidBase2ConstantsShouldThrow() {
        parseInvalidIntValues("#0b", "#0B");
    }

    @Test
    void parseValidBase8ConstantsShouldSucceed() {
        parseValidIntValues("071", "052", 57, 42);
    }

    @Test
    void parseInvalidBase8ConstantsShouldThrow() {
        parseInvalidIntValues("08", "09");
    }

    @Test
    void parsePrefixedValidBase8ConstantsShouldSucceed() {
        parseValidIntValues("#071", "#052", 57, 42);
    }

    @Test
    void parsePrefixedInvalidBase8ConstantsShouldThrow() {
        parseInvalidIntValues("#08", "#09");
    }

    @Test
    void parseValidBase10ConstantsShouldSucceed() {
        parseValidIntValues("71", "52", 71, 52);
    }

    @Test
    void parseInvalidBase10ConstantsShouldThrow() {
        parseInvalidIntValues("1a", "1b");
    }

    @Test
    void parsePrefixedValidBase10ConstantsShouldSucceed() {
        parseValidIntValues("#71", "#52", 71, 52);
    }

    @Test
    void parsePrefixedInvalidBase10ConstantsShouldThrow() {
        parseInvalidIntValues("#1a", "#1b");
    }

    @Test
    void parseValidBase16ConstantsShouldSucceed() {
        parseValidIntValues("0x71af", "0X52EF", 0x71af, 0x52ef);
    }

    @Test
    void parseInvalidBase16ConstantsShouldThrow() {
        parseInvalidIntValues("0xay", "0xaz");
    }

    @Test
    void parsePrefixedValidBase16ConstantsShouldSucceed() {
        parseValidIntValues("#0x71af", "#0X52EF", 0x71af, 0x52ef);
    }

    @Test
    void parsePrefixedInvalidBase16ConstantsShouldThrow() {
        parseInvalidIntValues("#0xay", "#0xaz");
    }

    @Test
    void parseConstantMemLocShouldReturnValidConstMemLoc() {
        var parser = new Parser();
        var p0 = parser.parseInstruction(0, "add [100] r0;").getInstructions().getFirst().getParam1();
        assertInstanceOf(ConstantMemoryLocation.class, p0);
        assertEquals((char) 100, p0.getValue());
    }

    @Test
    void parseAbsoluteMemLocShouldReturnValidAbsMemLoc() {
        var parser = new Parser();
        var cpu = new CentralProcessingUnit();
        var regRef = cpu.getRegistryReferenceMap();
        var r1 = cpu.getDataRegisters().get(1);
        var p0 = parser.parseInstruction(0, "add [r1] r0;")
                .resolveReferences(regRef).getInstructions().getFirst().getParam1();
        assertInstanceOf(AbsoluteMemoryLocation.class, p0);
        assertEquals((char) 0, p0.getValue());
        r1.setValue((char) 50);
        assertEquals((char) 50, p0.getValue());
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