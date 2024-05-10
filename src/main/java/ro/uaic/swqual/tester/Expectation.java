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
import java.util.regex.Pattern;

public class Expectation {
    private static final String EXPECT_TRUE_ID = "expect-true";
    private static final String EXPECT_FALSE_ID = "expect-false";

    private final Predicate<Expression> predicate;
    private final List<Expression> expressions;

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
                "(" + EXPECT_TRUE_ID + "|" + EXPECT_FALSE_ID + ")"
                + " \\{([^}]*)}"
        );

        var matcher = pattern.matcher(expectationString);
        if (!matcher.find()) {
            return null;
        }

        return Optional.ofNullable(Map.of(
                EXPECT_TRUE_ID, expectTrue(),
                EXPECT_FALSE_ID, expectFalse()
        ).get(matcher.group(1)))
                .orElseThrow(() -> new UndefinedExpectationException(matcher.group(1)))
                .addExpressions(
                        Arrays.stream(matcher.group(2).split(";"))
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
}
