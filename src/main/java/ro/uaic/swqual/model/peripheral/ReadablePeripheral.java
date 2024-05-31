package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.mem.ReadableMemoryUnit;

/**
 * Represents a peripheral that is only interactable via read-only addresses.
 * Also allows synchronizing it to the clock of the attached processing units.
 */
public interface ReadablePeripheral extends Peripheral, ReadableMemoryUnit {}
