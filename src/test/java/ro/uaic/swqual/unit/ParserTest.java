package ro.uaic.swqual.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.Parser;
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

class ParserTest {

    private Parser parser;

    @BeforeEach
    public void setUp() {
        parser = new Parser();
    }

    @Test
    void testParseInstructionLineShouldSucceed() {
        var line = "add r0 #7";
        var instruction = parser.parseInstruction(1, line);

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
        var instructionList = parser.parse(path);

        Assertions.assertEquals( 4, instructionList.size());
    }

    @Test
    void testParseInputFileShouldThrowException() {
        var path = "src/test/resources/unit/test-parser-failure.txt";
        Assertions.assertThrows(RuntimeException.class, () -> parser.parse(path));
    }

    @Test
    void testLinkJumpsShouldSucceed() {
        var path = "src/test/resources/unit/test-jmp.txt";
        var instructionList = parser.parse(path);
        parser.link();
        Assertions.assertEquals(5, instructionList.get(2).getParam1().getValue());
        Assertions.assertEquals(0, instructionList.get(7).getParam1().getValue());
    }

    @Test
    void testLinkJumpsShouldThrowNotFoundException() {
        var path = "src/test/resources/unit/test-jmp-failure.txt";
        parser.parse(path);
        Assertions.assertThrows(JumpLabelNotFoundException.class, () -> parser.link());
    }

    @Test
    void testParseShouldThrowDuplicateException() {
        var path = "src/test/resources/unit/test-jmp-failure-dup.txt";
        Assertions.assertThrows(DuplicateJumpTargetException.class ,() -> parser.parse(path));
    }

    @Test
    void testParseResolveReferencesShouldSucceed() {
        var code = Map.of(
                1, "add r0 r1",
                2, "sub r3 r4",
                3, "cmp r7 #1"
        );

        var parser = new Parser();
        var instructions = code.entrySet().stream().map(e -> parser.parseInstruction(e.getKey(), e.getValue())).toList();
        var cpu = new CentralProcessingUnit();

        Predicate<Instruction> containsUnresolvedReferences =
                (i) -> i.getParam1() instanceof RegisterReference || i.getParam2() instanceof RegisterReference;
        Predicate<Instruction> containsResolvedReferences =
                (i) -> i.getParam1() instanceof Register || i.getParam2() instanceof Register;

        Assertions.assertEquals(3, instructions.size());

        Assertions.assertTrue(instructions.stream().allMatch(containsUnresolvedReferences));
        Assertions.assertTrue(instructions.stream().noneMatch(containsResolvedReferences));

        var resolved = Parser.resolveReferences(instructions, cpu.getRegistryReferenceMap());
        Assertions.assertEquals(3, resolved.size());

        Assertions.assertTrue(instructions.stream().noneMatch(containsUnresolvedReferences));
        Assertions.assertTrue(instructions.stream().allMatch(containsResolvedReferences));
    }

    @Test
    void testParseResolveFirstReferenceShouldFail() {
        var code = Map.of(
                1, "@Label:",
                2, "add r0 r1",
                3, "jmp @Label",
                4, "sub r11 r4"
        );

        var parser = new Parser();
        var instructions = code.entrySet().stream().map(e -> parser.parseInstruction(e.getKey(), e.getValue())).toList();
        var cpu = new CentralProcessingUnit();

        Assertions.assertThrows(
                UndefinedReferenceException.class,
                () -> Parser.resolveReferences(instructions, cpu.getRegistryReferenceMap()),
                "Error at line 4: Undefined Reference to symbol 'r11'"
        );
    }

    @Test
    void testParseResolveSecondReferenceShouldFail() {
        var code = Map.of(
                1, "@Label:",
                2, "add r0 r1",
                3, "jmp @Label",
                4, "sub r4 r16"
        );

        var parser = new Parser();
        var instructions = code.entrySet().stream().map(e -> parser.parseInstruction(e.getKey(), e.getValue())).toList();
        var cpu = new CentralProcessingUnit();

        Assertions.assertThrows(
                UndefinedReferenceException.class,
                () -> Parser.resolveReferences(instructions, cpu.getRegistryReferenceMap()),
                "Error at line 4: Undefined Reference to symbol 'r16'"
        );
    }
}