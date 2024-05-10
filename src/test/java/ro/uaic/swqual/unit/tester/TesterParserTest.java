package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.Parser;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.proc.CPU;
import ro.uaic.swqual.tester.Expectation;
import ro.uaic.swqual.tester.TesterParser;
import ro.uaic.swqual.util.Function3;
import ro.uaic.swqual.util.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        var output = parser.parse(resource);
        parser.link();
        if (registerMap != null) {
            output = Parser.resolveReferences(output, registerMap);
        }
        consumer.accept(parser, output);
    }

    @Test
    void testerParserOutputShouldBeEquivalentToNormalParserOutput() {
        var resource0 = "src/test/resources/unit/test-parser.txt";
        var resource1 = "src/test/resources/unit/test-jmp.txt";
        var p0 = new Parser();
        var p1 = new TesterParser();
        var cpu = new CPU();
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
        var resource0 = "src/test/resources/unit/tester-parser-test.txt";
        parseResource(new TesterParser(), resource0, null, (parser, instructions) -> {
            var cpu = new CPU();
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
}
