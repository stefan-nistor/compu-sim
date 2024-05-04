package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ValueException;

import java.util.Objects;

public class Register extends Parameter {
    public short getValue() {
        return value;
    }
    public void setValue(short value) {
        this.value = value;
    }
    public void setValue(int value) throws ValueException {
        if (value <= 0xFFFF && value > Short.MAX_VALUE) {
            value = (value & 0x7FFF) | 0x8000;
        } else if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new ValueException("Constant value '" + value + "' out of range for register range");
        }
        this.value = (short) value;
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
