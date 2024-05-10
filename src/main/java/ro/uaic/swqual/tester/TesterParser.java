package ro.uaic.swqual.tester;

import ro.uaic.swqual.Parser;
import ro.uaic.swqual.model.Instruction;

import java.util.HashMap;
import java.util.Map;

public class TesterParser extends Parser {
    private final Map<Instruction, Expectation> expectationMap = new HashMap<>();

    @Override
    public Instruction parseInstruction(int lineIndex, String line) {
        var commentIndex = line.indexOf("//");
        var instruction = super.parseInstruction(lineIndex, line);
        if (commentIndex == -1) {
            return instruction;
        }

        var expectation = Expectation.from(line.substring(commentIndex + 2).trim());
        if (expectation == null) {
            return instruction;
        }

        expectationMap.put(instruction, expectation);
        return instruction;
    }

    public Map<Instruction, Expectation> getExpectationMap() {
        return expectationMap;
    }
}
