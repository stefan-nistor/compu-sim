package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ValueException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;

public class RelativeMemoryLocation extends MemoryLocation {
    private List<Parameter> parameters;
    private final List<BinaryOperator<Character>> relations;
    public RelativeMemoryLocation(List<Parameter> parameters, List<BinaryOperator<Character>> relations) throws ValueException {
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

    @Override
    public void resolveInnerReferences(Map<String, Register> registerMap) {
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

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
