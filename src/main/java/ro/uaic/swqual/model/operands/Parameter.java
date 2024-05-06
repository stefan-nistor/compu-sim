package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;

public abstract class Parameter {
    protected short value;
    public short getValue() {
        return value;
    }
    public void setValue(short value) throws ParameterException {
        throw new ParameterException("Attempted write in a non-writeable parameter");
    }
}
