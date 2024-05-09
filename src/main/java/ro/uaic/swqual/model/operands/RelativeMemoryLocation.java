package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ValueException;

import java.util.List;
import java.util.function.BinaryOperator;

public class RelativeMemoryLocation extends MemoryLocation {
    private final List<Parameter> parameters;
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
}
