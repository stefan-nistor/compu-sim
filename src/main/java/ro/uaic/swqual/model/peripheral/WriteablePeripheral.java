package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.mem.WriteableMemoryUnit;

/**
 * Represents a peripheral that is only interactable via write-only addresses.
 * Also allows synchronizing it to the clock of the attached processing units.
 */
public interface WriteablePeripheral extends Peripheral, WriteableMemoryUnit {}
