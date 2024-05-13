package ro.uaic.swqual.tester;

import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.model.operands.Register;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class ExpressionExpectation extends Expectation {
    private final Predicate<Expression> predicate;
    private final List<Expression> expressions;

    public ExpressionExpectation(Predicate<Expression> predicate, List<Expression> expressions) {
        this.predicate = predicate;
        this.expressions = expressions;
    }

    public ExpressionExpectation addExpressions(List<Expression> expressions) {
        this.expressions.addAll(expressions);
        return this;
    }

    public ExpressionExpectation referencing(Map<String, Register> registerMap) {
        expressions.forEach(expr -> expr.resolveReferences(registerMap));
        return this;
    }

    @Override
    public boolean evaluate() {
        return expressions.stream().filter(predicate).count() == expressions.size();
    }

    @Override
    protected void load(String data) {
        addExpressions(
                Arrays.stream(data.split(";"))
                        .map(String::trim)
                        .map(Expression::from)
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    public void readAddressesFrom(ReadableMemoryUnit unit, Character begin, Character end) {
        expressions.forEach(expr -> expr.readAddressesFrom(unit, begin, end));
    }

    @Override
    public String dump() {
        var builder = new StringBuilder();
        for (var expr : expressions) {
            builder.append("\tExpression '").append(expr.getCode()).append("'. ").append(expr.dump()).append('\n');
        }
        return builder.toString();
    }
}
