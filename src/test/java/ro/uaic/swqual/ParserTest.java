package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.Processor;
import ro.uaic.swqual.exception.parser.DuplicateJumpTargetException;
import ro.uaic.swqual.exception.parser.JumpLabelNotFoundException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.RegisterReference;

import java.util.List;
import java.util.function.Predicate;

public class ParserTest {

    private Parser parser;

    @Before
    public void setUp() {
        parser = new Parser();
    }

    @Test
    public void testParseInstructionLineShouldSucceed() {
        var line = "add r0 #7";
        var instruction = parser.parseInstruction(line);

        Assert.assertEquals(InstructionType.ALU_ADD, instruction.getType());
        Assert.assertEquals(2, instruction.getParameters().size());
        Assert.assertEquals(7, instruction.getParam2().getValue());
        Assert.assertTrue(instruction.getParam1() instanceof RegisterReference);
        Assert.assertEquals("r0", ((RegisterReference) instruction.getParam1()).getName());
    }

    @Test
    public void testParseInputFileShouldSucceed() {
        var path = "src/test/resources/test-parser.txt";
        var instructionList = parser.parse(path);

        Assert.assertEquals( 4, instructionList.size());
    }

    @Test
    public void testParseInputFileShouldThrowException() {
        var path = "src/test/resources/test-parser-failure.txt";
        Assert.assertThrows(RuntimeException.class, () -> parser.parse(path));
    }

    @Test
    public void testLinkJumpsShouldSucceed() {
        var path = "src/test/resources/test-jmp.txt";
        var instructionList = parser.parse(path);
        parser.link();
        Assert.assertEquals(5, instructionList.get(2).getParam1().getValue());
        Assert.assertEquals(0, instructionList.get(7).getParam1().getValue());
    }

    @Test
    public void testLinkJumpsShouldThrowNotFoundException() {
        var path = "src/test/resources/test-jmp-failure.txt";
        parser.parse(path);
        Assert.assertThrows(JumpLabelNotFoundException.class, () -> parser.link());
    }

    @Test
    public void testParseShouldThrowDuplicateException() {
        var path = "src/test/resources/test-jmp-failure-dup.txt";
        Assert.assertThrows(DuplicateJumpTargetException.class ,() -> parser.parse(path));
    }

    @Test
    public void testParseResolveReferencesShouldSucceed() {
        var code = List.of(
                "add r0 r1",
                "sub r3 r4",
                "cmp r7 #1"
        );

        var parser = new Parser();
        var instructions = code.stream().map(parser::parseInstruction).toList();
        var cpu = new Processor();

        Predicate<Instruction> containsUnresolvedReferences =
                (i) -> i.getParam1() instanceof RegisterReference || i.getParam2() instanceof RegisterReference;
        Predicate<Instruction> containsResolvedReferences =
                (i) -> i.getParam1() instanceof Register || i.getParam2() instanceof Register;

        Assert.assertEquals(3, instructions.size());

        Assert.assertTrue(instructions.stream().allMatch(containsUnresolvedReferences));
        Assert.assertTrue(instructions.stream().noneMatch(containsResolvedReferences));

        var resolved = Parser.resolveReferences(instructions, cpu.registryReferenceMap);
        Assert.assertEquals(3, resolved.size());

        Assert.assertTrue(instructions.stream().noneMatch(containsUnresolvedReferences));
        Assert.assertTrue(instructions.stream().allMatch(containsResolvedReferences));
    }
}