package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ro.uaic.swqual.exception.parser.DuplicateJumpTargetException;
import ro.uaic.swqual.exception.parser.JumpLabelNotFoundException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.proc.Processor;


public class ParserTest {

    private Parser parser;

    @Before
    public void setUp() {
        Processor processor = new Processor();
        parser = new Parser(processor);
    }

    @Test
    public void testParseInstructionLineShouldSucceed() {
        var line = "add r0 #7";
        var instruction = parser.parseInstruction(line);

        Assert.assertEquals(InstructionType.ALU_ADD, instruction.getType());
        Assert.assertEquals(2, instruction.getParameters().size());
        Assert.assertEquals(7, instruction.getParam2().getValue());
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
}