package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ValueException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;

/**
 * Represents a memory value at an address identified by an expression. Will evaluate the expression when the address is requested.
 */
public class RelativeMemoryLocation extends MemoryLocation {
    /* The parameters involved in the expression */
    private List<Parameter> parameters;
    /* The relations between the parameters */
    private final List<BinaryOperator<Character>> relations;

    /**
     * Primary constructor
     * @param parameters list of parameters in the expression
     * @param relations list of relations between the parameters
     * @throws ValueException if parameter and relations lists do not form a valid expression.
     */
    public RelativeMemoryLocation(List<Parameter> parameters, List<BinaryOperator<Character>> relations) throws ValueException {
        assert parameters != null;
        assert relations != null;
        this.parameters = parameters;
        this.relations = relations;
        if (this.parameters.size() != this.relations.size() + 1) {
            throw new ValueException(
                    "Illegal composition of relative memory location. " +
                    "Expected n relations and n + 1 parameters. " +
                    "Received '" + this.parameters.size() + "' parameters and '" + this.relations.size() + "' relations" +
                    "Reason: p0 r0 p1 r1 p2 ... rn pn+1 is the correct formula. " +
                    "Example: [r0 + 14 * r1]"
            );
        }
    }

    /**
     * Getter for address. Will evaluate expression
     * @return the address currently resolved from the expression.
     */
    @Override
    public char getValue() {
        var resolved = parameters.getFirst().getValue();
        for (var index = 0; index < relations.size(); ++index) {
            resolved = relations.get(index).apply(resolved, parameters.get(index + 1).getValue());
        }
        return resolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelativeMemoryLocation that = (RelativeMemoryLocation) o;
        var paramEqual = Objects.equals(parameters, that.parameters);
        var relEqual = Objects.equals(relations, that.relations);
        return paramEqual && relEqual;
    }

    /**
     * Method that allows resolving any {@link RegisterReference} to {@link Register}
     * that may be part of the expression resolving the memory address.
     * @param registerMap name to {@link Register} dictionary.
     */
    @Override
    public void resolveInnerReferences(Map<String, Register> registerMap) {
        assert registerMap != null;
        parameters = parameters.stream().map(location -> {
            if (location instanceof RegisterReference ref) {
                var referee = registerMap.get(ref.getName());
                if (referee != null) {
                    return referee;
                }
            }
            return location;
        }).toList();
    }

    @Override
    public String toString() {
        return "[<relative-location>] (=0x" + Integer.toString(getValue(), 16) + ")";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
