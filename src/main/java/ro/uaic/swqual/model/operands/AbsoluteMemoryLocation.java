package ro.uaic.swqual.model.operands;

public class AbsoluteMemoryLocation extends MemoryLocation {
    final Parameter location;

    public AbsoluteMemoryLocation(Parameter parameter) {
        this.location = parameter;
    }

    @Override
    public char getValue() {
        return location.getValue();
    }
}
