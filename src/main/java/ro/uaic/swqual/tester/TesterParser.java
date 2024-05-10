package ro.uaic.swqual.tester;

import ro.uaic.swqual.Parser;
import ro.uaic.swqual.exception.parser.UndefinedReferenceException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Register;

import java.util.HashMap;
import java.util.Map;

public class TesterParser extends Parser {
    private final Map<Instruction, Expectation> expectationMap = new HashMap<>();

    @Override
    public TesterParser parseInstruction(int lineIndex, String line) {
        var commentIndex = line.indexOf("//");
        super.parseInstruction(lineIndex, line);
        if (commentIndex == -1) {
            return this;
        }

        var expectation = Expectation.from(line.substring(commentIndex + 2).trim());
        if (expectation == null) {
            return this;
        }

        var instruction = super.getInstructions().getLast();
        expectationMap.put(instruction, expectation);
        return this;
    }

    public Map<Instruction, Expectation> getExpectationMap() {
        return expectationMap;
    }

    @Override
    public Parser resolveReferences(Map<String, Register> registerMap) throws UndefinedReferenceException {
        super.resolveReferences(registerMap);

        // Force a rehash since instructions hash codes have changed
        var entries = expectationMap.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().referencing(registerMap)))
                .toList();
        expectationMap.clear();
        entries.forEach(entry -> expectationMap.put(entry.getKey(), entry.getValue()));
        return this;
    }
}
