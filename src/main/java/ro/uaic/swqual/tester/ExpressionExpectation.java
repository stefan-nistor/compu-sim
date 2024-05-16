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
        assert predicate != null;
        assert expressions != null;
        this.predicate = predicate;
        this.expressions = expressions;
    }

    public ExpressionExpectation addExpressions(List<Expression> expressions) {
        assert expressions != null;
        this.expressions.addAll(expressions);
        return this;
    }

    public ExpressionExpectation referencing(Map<String, Register> registerMap) {
        assert registerMap != null;
        expressions.forEach(expr -> expr.resolveReferences(registerMap));
        return this;
    }

    @Override
    public boolean evaluate() {
        return expressions.stream().filter(predicate).count() == expressions.size();
    }

    @Override
    protected void load(String data) {
        assert data != null;
        addExpressions(
                Arrays.stream(data.split(";"))
                        .map(String::trim)
                        .map(Expression::from)
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    public void readAddressesFrom(ReadableMemoryUnit unit, Character begin, Character end) {
        assert unit != null;
        assert begin != null;
        assert end != null;
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
