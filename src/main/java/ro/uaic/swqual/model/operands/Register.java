package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ValueException;

/**
 * Represents a readable/writeable parameter. Smallest concrete memory unit inside a processing unit.
 */
public class Register extends Parameter {
    /**
     * Value setter
     * @param value new value to be set
     */
    @Override
    public void setValue(char value) {
        // Allow Registers to be writeable
        this.value = value;
    }

    /**
     * Value setter overridden for convenience. Applies value limitations.
     * @param value new value to be set. Expected to be in unsigned char value range [0, 65536)
     * @throws ValueException when value is not in the expected range
     */
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
    public String toString() {
        return "reg(" + (int) value + ")";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
