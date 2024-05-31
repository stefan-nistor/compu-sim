package ro.uaic.swqual.tester;

import ro.uaic.swqual.Parser;
import ro.uaic.swqual.exception.parser.ParserException;
import ro.uaic.swqual.exception.parser.UndefinedReferenceException;
import ro.uaic.swqual.exception.tester.InvalidHeaderItemException;
import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.RegisterReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents the parser used by the {@link Tester} framework. Allows parsing {@link Expectation Expectations}
 * Files used by the Tester Framework require a header to be present. <br/>
 * A valid header is the first non-whitespace text that is a comment group, starting with 'sim-test' <br/>
 * Example header:
 * <pre>
 * // sim-test
 * // expected: success
 *
 * mov r0 r1;
 * </pre>
 * This is used to extract data regarding the test.
 */
public class TesterParser extends Parser {
    /** Header read state */
    private enum State {
        /** No header detected yet */
        NO_HEADER_DETECTED,
        /** Currently parsing header */
        IN_HEADER,
        /** Moved past the possible header data */
        PASSED_HEADER
    }

    /** Map identifying {@link Expectation} objects to the {@link Instruction} to be evaluated after execution */
    private final Map<Instruction, Expectation> expectationMap = new HashMap<>();
    /** Current header parse state */
    private State state = State.NO_HEADER_DETECTED;
    /** Test expected outcome */
    private boolean expectedToSucceed = false;
    /** {@link ro.uaic.swqual.model.peripheral.Keyboard Keyboard} preload listeners.
     *  The checked files can contain '// kb-preload {...}' comments.
     *  The data is relayed back to the listeners in this list */
    private final List<Consumer<List<Parameter>>> onKbPreloadListeners = new ArrayList<>();

    /**
     * Method used to add a keyboard-preload listener.
     * @param listener the listener to be added.
     */
    public void addOnKbPreloadListener(Consumer<List<Parameter>> listener) {
        assert listener != null;
        onKbPreloadListeners.add(listener);
    }

    /**
     * Method used to parse a header item text.
     * @param itemText the text containing header item data.
     */
    private void parseHeaderItem(String itemText) {
        assert itemText != null;
        // parse test expectation data
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

        // anything else is invalid
        throw new InvalidHeaderItemException("Invalid header item: " + itemText);
    }

    /**
     * Method used to check whether the current checked file is expected to succeed or not
     * @return true if expected to succeed, false otherwise
     */
    public boolean isExpectedToSucceed() {
        return expectedToSucceed;
    }

    /**
     * Method used to parse a single line with a known line number. <br/>
     * Checks for the header parse, kb-preload. This is then trimmed,
     *   and the {@link Parser#parseLine} is then invoked
     * @param line the line to parse
     * @param lineIdx the line number
     */
    @Override
    protected void parseLine(String line, int lineIdx) {
        assert line != null;
        line = line.trim();
        // First non-comment gets us past the header. Also return if empty.
        if (line.isEmpty()) {
            state = State.PASSED_HEADER;
            return;
        }

        // First non-comment gets us past the header.
        if (!line.startsWith("//")) {
            state = State.PASSED_HEADER;
        } else if (line.startsWith("// kb-preload")) {
            // If kb-preload, extract data and call listeners.
            var lBracketIdx = line.indexOf('{');
            var rBracketIdx = line.indexOf('}');
            if (lBracketIdx == -1 || rBracketIdx == -1) {
                throw new ParserException("Expected 'kb-preload' to have {values...}");
            }

            // The data is formatted as characters/values separated by commas.
            String[] tokens = line.substring(lBracketIdx + 1, rBracketIdx).split(",");
            onKbPreloadListeners.forEach(listener -> listener.accept(
                    Arrays.stream(tokens)
                            .map(String::trim)
                            .map(l -> Parameter.parse(lineIdx, l))
                            .toList()
            ));
            return;
        }

        // Finally, check if we still have a header to parse. If not, move onto default implementation.
        switch (state) {
            case PASSED_HEADER -> super.parseLine(line, lineIdx);
            case NO_HEADER_DETECTED -> state = line.equals("// sim-test") ? State.IN_HEADER : State.NO_HEADER_DETECTED;
            case IN_HEADER -> parseHeaderItem(line.substring("// ".length()));
        }
    }

    /**
     * Method used to parse a definite instruction from a given line <br/>
     * Will check for expectations, and create and associate them. <br/>
     * Will parse the instruction via {@link Parser#parseInstruction} beforehand regardless of
     * whether an {@link Expectation} was found or not, using the text with the expectation data trimmed. <br/>
     * After this, linking of {@link Instruction} and {@link Expectation} occurs, if applicable.
     * @param lineIndex the line number
     * @param line the instruction code
     * @return reference to self used in chain operations
     */
    @Override
    public TesterParser parseInstruction(int lineIndex, String line) {
        assert line != null;
        // attempt to find expectation comment
        var commentIndex = line.indexOf("//");
        if (commentIndex == -1) {
            // if none, move onto instruction parse
            super.parseInstruction(lineIndex, line);
            return this;
        }

        // obtain the instruction left of the expectation
        super.parseInstruction(lineIndex, line.substring(0, commentIndex));
        // extract the expectation
        var expectation = Expectation.from(line.substring(commentIndex + 2).trim());
        if (expectation == null) {
            return this;
        }
        expectation.setLineHint(lineIndex);

        var instruction = super.getInstructions().getLast();
        // associate the two
        expectationMap.put(instruction, expectation);
        return this;
    }

    /**
     * {@link TesterParser#expectationMap} getter
     * @return the map of {@link Instruction} to {@link Expectation} objects
     */
    public Map<Instruction, Expectation> getExpectationMap() {
        return expectationMap;
    }

    /**
     * Method used to resolve the out-of-source references.
     * This is not a full reference resolve, as it will only cover the external references ({@link RegisterReference}).
     * <br/>
     * Inner {@link Expectation Expectations} might contain references as well, so resolve those alongside calling
     *   the base {@link Parser#resolveReferences}.
     * @param registerMap a map associating register assembly labels to actual {@link Register} objects.
     * @return Reference to self for use in chain operations
     * @throws UndefinedReferenceException if a {@link RegisterReference} in the current list of instructions cannot
     *   be resolved using the received map.
     */
    @Override
    public Parser resolveReferences(Map<String, Register> registerMap) throws UndefinedReferenceException {
        assert registerMap != null;
        // call base to resolve Instruction References
        super.resolveReferences(registerMap);
        // go through expectations and resolve references there as well.
        expectationMap.values().forEach(v -> {
            if (v instanceof ExpressionExpectation ee) {
                ee.referencing(registerMap);
            }
        });
        return this;
    }

    /**
     * Method used to provide access to a {@link ReadableMemoryUnit} to read and store the memory state from
     * @param unit the unit in question
     * @param begin the start of the unit's address space
     * @param end the end of the unit's address space
     */
    public void readAddressesFrom(ReadableMemoryUnit unit, Character begin, Character end) {
        assert unit != null;
        assert begin != null;
        assert end != null;
        // provide the unit to each found expectation.
        expectationMap.values().forEach(expectation -> {
            // right now, only ExpressionExpectations make use of this.
            if (expectation instanceof ExpressionExpectation ee) {
                ee.readAddressesFrom(unit, begin, end);
            }
        });
    }
}
