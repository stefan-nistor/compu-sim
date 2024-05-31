package ro.uaic.swqual.tester;

import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.model.operands.Register;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents an expectation that is composed of a group of expressions: // expect-true {r0 == r1; r4 == 5}
 */
public class ExpressionExpectation extends Expectation {
    /** The root {@link Predicate} acquired from the expression base,
     *  applied over every {@link Expression} part of the expectation */
    private final Predicate<Expression> predicate;
    /** {@link Expression Expressions} that are evaluated by the expectation */
    private final List<Expression> expressions;

    /**
     * Primary constructor
     * @param predicate the given {@link Predicate} that will validate each {@link Expression}
     * @param expressions the list of {@link Expression expressions} to validate.
     */
    public ExpressionExpectation(Predicate<Expression> predicate, List<Expression> expressions) {
        assert predicate != null;
        assert expressions != null;
        this.predicate = predicate;
        this.expressions = expressions;
    }

    /**
     * Method used to add {@link Expression expressions} to the current list
     * @param expressions list of new expressions to evaluate
     * @return Reference to self to use in chaining expressions
     */
    public ExpressionExpectation addExpressions(List<Expression> expressions) {
        assert expressions != null;
        this.expressions.addAll(expressions);
        return this;
    }

    /**
     * Method used to resolve references inside the {@link Expression expressions} part of the current expectation.
     * @param registerMap the map from assembly labels to actual {@link Register} objects
     * @return Reference to self to use in chaining expressions
     */
    public ExpressionExpectation referencing(Map<String, Register> registerMap) {
        assert registerMap != null;
        expressions.forEach(expr -> expr.resolveReferences(registerMap));
        return this;
    }

    /**
     * Method used to invoke the expectation evaluation.
     * @return true if expectation evaluated as-expected, false otherwise
     */
    @Override
    public boolean evaluate() {
        return expressions.stream().filter(predicate).count() == expressions.size();
    }

    /**
     * Method used to load the expectation data after creation.
     * @param data the data ({...}) to populate the expectation with
     */
    @Override
    protected void load(String data) {
        // for each expression separated by ';', parse it individually and only keep the non-null ones.
        assert data != null;
        addExpressions(
                Arrays.stream(data.split(";"))
                        .map(String::trim)
                        .map(Expression::from)
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    /**
     * Method used to provide access to a {@link ReadableMemoryUnit} to store the memory state from
     * @param unit the unit in question
     * @param begin the start of the unit's address space
     * @param end the end of the unit's address space
     */
    public void readAddressesFrom(ReadableMemoryUnit unit, Character begin, Character end) {
        assert unit != null;
        assert begin != null;
        assert end != null;
        expressions.forEach(expr -> expr.readAddressesFrom(unit, begin, end));
    }

    /**
     * Method used in dumping the state of the expectation at the moment of evaluation
     * @return string containing dump data.
     */
    @Override
    public String dump() {
        var builder = new StringBuilder();
        // dumps each expression to the output.
        for (var expr : expressions) {
            builder.append("\tExpression '").append(expr.getCode()).append("'. ").append(expr.dump()).append('\n');
        }
        return builder.toString();
    }
}
