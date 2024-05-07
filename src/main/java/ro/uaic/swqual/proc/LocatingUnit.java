package ro.uaic.swqual.proc;

import ro.uaic.swqual.model.operands.Parameter;

public interface LocatingUnit {
    Parameter locate(Parameter writeableOrLocation);
}
