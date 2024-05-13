package ro.uaic.swqual.tester;

import ro.uaic.swqual.Parser;
import ro.uaic.swqual.exception.parser.UndefinedReferenceException;
import ro.uaic.swqual.exception.tester.InvalidHeaderItemException;
import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Register;

import java.util.HashMap;
import java.util.Map;

public class TesterParser extends Parser {
    private enum State {
        NO_HEADER_DETECTED,
        IN_HEADER,
        PASSED_HEADER
    }

    private final Map<Instruction, Expectation> expectationMap = new HashMap<>();
    private State state = State.NO_HEADER_DETECTED;
    private boolean expectedToSucceed = false;

    private void parseHeaderItem(String itemText) {
        if (itemText.startsWith("expected: ")) {
            var expectation = itemText.substring("expected: ".length());
            if (expectation.equals("success")) {
                expectedToSucceed = true;
            } else if (expectation.equals("failure")) {
                expectedToSucceed = false;
            } else {
                throw new InvalidHeaderItemException("Invalid expectation: " + expectation);
            }

            return;
        }

        throw new InvalidHeaderItemException("Invalid header item: " + itemText);
    }

    public boolean isExpectedToSucceed() {
        return expectedToSucceed;
    }

    @Override
    protected void parseLine(String line, int lineIdx) {
        line = line.trim();
        if (line.isEmpty()) {
            state = State.PASSED_HEADER;
            return;
        }

        if (!line.startsWith("//")) {
            state = State.PASSED_HEADER;
        }

        switch (state) {
            case PASSED_HEADER -> super.parseLine(line, lineIdx);
            case NO_HEADER_DETECTED -> state = line.equals("// sim-test") ? State.IN_HEADER : State.NO_HEADER_DETECTED;
            case IN_HEADER -> parseHeaderItem(line.substring("// ".length()));
        }
    }

    @Override
    public TesterParser parseInstruction(int lineIndex, String line) {
        var commentIndex = line.indexOf("//");
        if (commentIndex == -1) {
            super.parseInstruction(lineIndex, line);
            return this;
        }

        super.parseInstruction(lineIndex, line.substring(0, commentIndex));
        var expectation = Expectation.from(line.substring(commentIndex + 2).trim());
        if (expectation == null) {
            return this;
        }
        expectation.setLineHint(lineIndex);

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
        expectationMap.values().forEach(v -> v.referencing(registerMap));
        // Force a rehash since instructions hash codes may have changed
        var entries = expectationMap.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().referencing(registerMap)))
                .toList();
        expectationMap.clear();
        entries.forEach(entry -> expectationMap.put(entry.getKey(), entry.getValue()));
        return this;
    }

    public void readAddressesFrom(ReadableMemoryUnit unit, Character begin, Character end) {
        expectationMap.values().forEach(expectation -> expectation.readAddressesFrom(unit, begin, end));
    }
}
