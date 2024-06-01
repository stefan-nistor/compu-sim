package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.proc.ClockListener;

/**
 * Represents a peripheral, operated by accessing addresses.
 * Also allows synchronizing it to the clock of the attached processing units.
 */
public interface Peripheral extends ClockListener, MemoryUnit {}
