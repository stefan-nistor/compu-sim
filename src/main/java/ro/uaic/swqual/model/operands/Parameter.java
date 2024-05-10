package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

import java.util.Objects;

public abstract class Parameter {
    /** As it happens, char = unsigned short. */
    protected char value;
    public char getValue() {
        return value;
    }

    public void setValue(char value) throws ParameterException {
        // By default, make all Parameter types non-writeable
        throw new ParameterException("Attempted write in a non-writeable parameter");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return value == parameter.value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
