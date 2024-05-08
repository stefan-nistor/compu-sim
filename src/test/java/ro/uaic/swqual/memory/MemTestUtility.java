package ro.uaic.swqual.memory;

import ro.uaic.swqual.TestUtility;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.DirectMemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;

public interface MemTestUtility extends TestUtility {
    default AbsoluteMemoryLocation aloc(Parameter reg) {
        return new AbsoluteMemoryLocation(reg);
    }
    default DirectMemoryLocation dloc(char value) { return new DirectMemoryLocation(value); }
}
