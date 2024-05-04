package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterError;
import ro.uaic.swqual.exception.ValueException;

public abstract class Parameter {
    protected short value;
    public short getValue() {
        return value;
    }
    public void setValue(short value) throws ParameterError {
        throw new ParameterError("Attempted write in a non-writeable parameter");
    }
}
