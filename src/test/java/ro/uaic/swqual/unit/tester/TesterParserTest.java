package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.Parser;
import ro.uaic.swqual.exception.tester.InvalidHeaderItemException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.CentralProcessingUnit;
import ro.uaic.swqual.tester.Expectation;
import ro.uaic.swqual.tester.TesterParser;
import ro.uaic.swqual.util.Function3;
import ro.uaic.swqual.util.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesterParserTest {
    interface ParseConsumer<T extends Parser> {
        void accept(T parser, List<Instruction> output);
    }

    <T extends Parser> void parseResource(
            T parser,
            String resource,
            Map<String, Register> registerMap,
            ParseConsumer<T> consumer
    ) {
        var output = parser.parse(resource).getInstructions();
        parser.link();
        if (registerMap != null) {
            parser.resolveReferences(registerMap);
        }
        consumer.accept(parser, output);
    }

    @Test
    void testerParserOutputShouldBeEquivalentToNormalParserOutput() {
        var resource0 = "src/test/resources/unit/test-parser.txt";
        var resource1 = "src/test/resources/unit/test-jmp.txt";
        var p0 = new Parser();
        var p1 = new TesterParser();
        var cpu = new CentralProcessingUnit();
        var m0 = cpu.getRegistryReferenceMap();

        Function3<Parser, String, Map<String, Register>, List<Instruction>> getOut = (p, r, m) -> {
            List<Instruction> outs = new ArrayList<>();
            parseResource(p, r, m, (p2, o) -> outs.addAll(o));
            return outs;
        };

        assertTrue(
                Stream.of(resource0, resource1)
                        .map(r -> Tuple.of(getOut.apply(p0, r, m0), getOut.apply(p1, r, m0)))
                        .allMatch(t -> t.map(List::equals))
        );
    }

    @Test
    void testerParserShouldGatherValidExpectationsCorrectly() {
        var resource0 = "src/test/resources/unit/tester-parser-gather-test.txt";
        parseResource(new TesterParser(), resource0, null, (parser, instructions) -> {
            var cpu = new CentralProcessingUnit();
            var regs = cpu.getDataRegisters();
            var refs = cpu.getRegistryReferenceMap();
            var expectations = parser.getExpectationMap();
            assertEquals(2, expectations.size());

            expectations.values().forEach(e -> e.referencing(refs));
            regs.get(0).setValue((char) 10);
            regs.get(1).setValue((char) 10);
            regs.get(2).setValue((char) 10);
            assertTrue(expectations.values().stream().allMatch(Expectation::evaluate));
        });
    }

    @Test
    void testerParserShouldIdentifySuccessExpectationCorrectly() {
        var resource0 = "src/test/resources/unit/tester-parser-success-expectation.txt";
        parseResource(new TesterParser(), resource0, null,
                (parser, instructions) -> assertTrue(parser.isExpectedToSucceed())
        );
    }

    @Test
    void testerParserShouldIdentifyFailureExpectationCorrectly() {
        var resource0 = "src/test/resources/unit/tester-parser-failure-expectation.txt";
        parseResource(new TesterParser(), resource0, null,
                (parser, instructions) -> assertFalse(parser.isExpectedToSucceed())
        );
    }

    @Test
    void testerParserShouldIdentifyInvalidExpectationAndThrow() {
        var resource0 = "src/test/resources/unit/tester-parser-invalid-expectation.txt";
        assertThrows(
                InvalidHeaderItemException.class,
                () -> parseResource(new TesterParser(), resource0, null, (parser, instructions) -> {}),
                "Invalid expectation: unknown"
        );
    }

    @Test
    void testerParserShouldIdentifyInvalidHeaderItemAndThrow() {
        var resource0 = "src/test/resources/unit/tester-parser-invalid-header.txt";
        assertThrows(
                InvalidHeaderItemException.class,
                () -> parseResource(new TesterParser(), resource0, null, (parser, instructions) -> {}),
                "Invalid header item: something: success"
        );
    }
}
