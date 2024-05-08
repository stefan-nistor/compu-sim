package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

public class Label extends Parameter {
    private final String name;

    public Label(String label) {
        this.name = label;
    }

    public String getName() {
        return name;
    }

    @Override public char getValue() throws ParameterException {
        throw new ParameterException("Label does not contain an actual numeric value");
    }
}
