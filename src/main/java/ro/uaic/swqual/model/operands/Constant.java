package ro.uaic.swqual.model.operands;

public class Constant extends Parameter {
    public Constant(char value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "" + (int) value;
    }
}
