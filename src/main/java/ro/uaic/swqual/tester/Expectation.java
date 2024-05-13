package ro.uaic.swqual.tester;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static ro.uaic.swqual.tester.Expression.EvaluationType.FALSE;
import static ro.uaic.swqual.tester.Expression.EvaluationType.TRUE;

public abstract class Expectation {
    private static final Map<String, Supplier<Expectation>> EXPECTATION_SUPPLIERS = Map.of(
            "expect-true", Expectation::expectTrue,
            "expect-false", Expectation::expectFalse,
            "expect-display", Expectation::predicateExpectation
    );

    public static Expectation expectTrue() {
        return new ExpressionExpectation(expression -> expression.evaluate() == TRUE, new ArrayList<>());
    }

    public static Expectation expectFalse() {
        return new ExpressionExpectation(expression -> expression.evaluate() == FALSE, new ArrayList<>());
    }

    public static Expectation predicateExpectation() {
        return new PredicateExpectation();
    }

    private int line;
    private String code;
    protected String tag;

    protected void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public static Expectation from(String expectationString) {
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
        expectation.load(matcher.group(2));
        return expectation;
    }

    public abstract boolean evaluate();
    public abstract String dump();
    protected abstract void load(String data);

    public Expectation setLineHint(int line) {
        this.line = line;
        return this;
    }

    public int getLine() {
        return line;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
