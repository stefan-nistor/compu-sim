package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ValueException;

import java.util.Objects;

public class Register extends Parameter {
    @Override
    public void setValue(char value) {
        // Allow Registers to be writeable
        this.value = value;
    }

    public void setValue(int value) throws ValueException {
        if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) {
            throw new ValueException("Constant value '" + value + "' out of range for register range");
        }
        this.value = (char) value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Register register = (Register) o;
        return value == register.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
