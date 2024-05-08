package ro.uaic.swqual.mem;

import ro.uaic.swqual.TestUtility;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;

public interface MemTestUtility extends TestUtility {
    default AbsoluteMemoryLocation aloc(Parameter reg) {
        return new AbsoluteMemoryLocation(reg);
    }
    default ConstantMemoryLocation dloc(char value) { return new ConstantMemoryLocation(value); }
}
