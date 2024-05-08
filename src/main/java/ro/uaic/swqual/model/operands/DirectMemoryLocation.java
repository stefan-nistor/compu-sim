package ro.uaic.swqual.model.operands;

public class DirectMemoryLocation extends MemoryLocation {
    final char value;

    public DirectMemoryLocation(char value) {
        this.value = value;
    }

    @Override
    public char getValue() {
        return value;
    }
}
