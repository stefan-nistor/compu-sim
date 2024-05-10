package ro.uaic.swqual.tester;

import ro.uaic.swqual.exception.tester.UndefinedExpectationException;
import ro.uaic.swqual.model.operands.Register;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Expectation {
    private static final Map<String, Supplier<Expectation>> EXPECTATION_SUPPLIERS = Map.of(
        "expect-true", Expectation::expectTrue,
        "expect-false", Expectation::expectFalse
    );

    private final Predicate<Expression> predicate;
    private final List<Expression> expressions;
    private int line;
    private String code;

    private Expectation(Predicate<Expression> predicate, List<Expression> expressions) {
        this.predicate = predicate;
        this.expressions = expressions;
    }

    public static Expectation expectTrue() {
        return new Expectation(Expression::evaluate, new ArrayList<>());
    }

    public static Expectation expectFalse() {
        return new Expectation(Predicate.not(Expression::evaluate), new ArrayList<>());
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

        return Optional.ofNullable(Optional.ofNullable(EXPECTATION_SUPPLIERS.get(matcher.group(1)))
                .orElse(() -> null).get())
                .orElseThrow(() -> new UndefinedExpectationException(matcher.group(1)))
                .setCode(expectationString)
                .addExpressions(
                        Arrays.stream(matcher.group(2).split(";"))
                                .map(String::trim)
                                .map(Expression::from)
                                .filter(Objects::nonNull)
                                .toList()
                );
    }

    public Expectation addExpressions(List<Expression> expressions) {
        this.expressions.addAll(expressions);
        return this;
    }

    public Expectation referencing(Map<String, Register> registerMap) {
        expressions.forEach(expr -> expr.resolveReferences(registerMap));
        return this;
    }

    public boolean evaluate() {
        return expressions.stream().allMatch(predicate);
    }

    public Expectation setLineHint(int line) {
        this.line = line;
        return this;
    }

    public int getLine() {
        return line;
    }

    public Expectation setCode(String code) {
        this.code = code;
        return this;
    }

    public String getCode() {
        return code;
    }

    public String dump() {
        var builder = new StringBuilder();
        for (var expr : expressions) {
            builder.append("\tExpression '").append(expr.getCode()).append("'. ").append(expr.dump()).append('\n');
        }
        return builder.toString();
    }
}
