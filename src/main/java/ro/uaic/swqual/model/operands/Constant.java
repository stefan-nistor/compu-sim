package ro.uaic.swqual.model.operands;

/**
 * Represents a constant value
 */
public class Constant extends Parameter {
    /**
     * Primary constructor
     * @param value to store in the constant
     */
    public Constant(char value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "" + (int) value;
    }
}
