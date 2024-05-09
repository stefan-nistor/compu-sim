package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

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
}
