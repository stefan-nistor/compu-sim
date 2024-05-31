package ro.uaic.swqual.tester;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static ro.uaic.swqual.tester.Expression.EvaluationType.FALSE;
import static ro.uaic.swqual.tester.Expression.EvaluationType.TRUE;

/**
 * Represents the expectation base found in expectation comments part of the tester framework.
 */
public abstract class Expectation {
    /** Map of concrete expectation suppliers. */
    private static final Map<String, Supplier<Expectation>> EXPECTATION_SUPPLIERS = Map.of(
            "expect-true", Expectation::expectTrue,
            "expect-false", Expectation::expectFalse,
            "expect-display", Expectation::predicateExpectation
    );

    /**
     * Method constructing an empty // expect-true {...} expectation
     * @return the newly constructed expectation
     */
    public static Expectation expectTrue() {
        return new ExpressionExpectation(expression -> expression.evaluate() == TRUE, new ArrayList<>());
    }

    /**
     * Method constructing an empty // expect-false {...} expectation
     * @return the newly constructed expectation
     */
    public static Expectation expectFalse() {
        return new ExpressionExpectation(expression -> expression.evaluate() == FALSE, new ArrayList<>());
    }

    /**
     * Method constructing an empty predicate-based expectation, such as // expect-display {...}
     * @return the newly constructed expectation
     */
    public static Expectation predicateExpectation() {
        return new PredicateExpectation();
    }

    /** line location of the expectation */
    private int line;
    /** source textual contents of the expectation */
    private String code;
    /** tag containing expectation identification (true/false/display...) */
    protected String tag;

    /**
     * Expectation tag setter
     * @param tag newly set tag
     */
    protected void setTag(String tag) {
        assert tag != null;
        this.tag = tag;
    }

    /**
     * Expectation tag getter
     * @return string containing the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Method used to obtain an expectation from a string containing matching code
     * @param expectationString the string containing the expectation code
     * @return the newly constructed expectation, or null if no such expectation could be constructed.
     */
    public static Expectation from(String expectationString) {
        assert expectationString != null;
        // match by finding the tag (expect-true/false/display) followed by capturing everything between {...}.
        var pattern = Pattern.compile(
                "(" + EXPECTATION_SUPPLIERS.keySet().stream().reduce((a, b) -> a + "|" + b).orElse("") + ")"
                + " \\{([^}]*)}"
        );

        var matcher = pattern.matcher(expectationString);
        if (!matcher.find()) {
            return null;
        }

        var expectationSupplier = EXPECTATION_SUPPLIERS.get(matcher.group(1));
        if (expectationSupplier == null) {
            return null;
        }

        var expectation = expectationSupplier.get();
        expectation.setCode(expectationString);
        expectation.setTag(matcher.group(1));
        // once a tag and evaluation data found, provide the expectation data ({...}) to the derived load logic.
        expectation.load(matcher.group(2));
        return expectation;
    }

    /**
     * Method used to invoke the expectation evaluation.
     * @return true if expectation evaluated as-expected, false otherwise
     */
    public abstract boolean evaluate();

    /**
     * Method used in dumping the state of the expectation at the moment of evaluation
     * @return string containing dump data.
     */
    public abstract String dump();

    /**
     * Method used to load the expectation data after creation.
     * @param data the data ({...}) to populate the expectation with
     */
    protected abstract void load(String data);

    /**
     * Setter for line number
     * @param line new line number
     * @return Reference to self to use in chaining operations
     */
    public Expectation setLineHint(int line) {
        this.line = line;
        return this;
    }

    /**
     * Getter for line number
     * @return line number
     */
    public int getLine() {
        return line;
    }

    /**
     * Method used to add the code that is representative of the expectation.
     * @param code the text containing the code representation
     */
    public void setCode(String code) {
        assert code != null;
        this.code = code;
    }

    /**
     * Getter for the expectation representative code.
     * @return string containing the code.
     */
    public String getCode() {
        return code;
    }
}
