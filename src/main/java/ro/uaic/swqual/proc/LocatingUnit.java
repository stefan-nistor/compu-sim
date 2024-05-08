package ro.uaic.swqual.proc;

import ro.uaic.swqual.model.operands.Parameter;

public interface LocatingUnit {
    void raiseFlag(char value);
    Parameter locate(Parameter writeableOrLocation);
}
