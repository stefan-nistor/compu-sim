package ro.uaic.swqual.proc;

import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.peripheral.Peripheral;

/**
 * Represents the Unit managing with Input/Output {@link Peripheral} Units
 */
public class InputOutputManagementUnit extends ProxyUnit<Peripheral> implements ClockListener {
    /** Reference to the {@link FlagRegister} to raise errors to */
    private final FlagRegister flagRegister;

    /**
     * Primary constructor
     * @param flagRegister reference to the {@link FlagRegister} to be used for raising status and errors
     */
    public InputOutputManagementUnit(FlagRegister flagRegister) {
        assert flagRegister != null;
        this.flagRegister = flagRegister;
    }

    /**
     * Method used to raise an error via a flag value, present in
     *   {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister}.
     * @param value flag value to raise.
     */
    @Override
    public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    /**
     * Method to be called on each clock tick.
     */
    @Override
    public void onTick() {
        // Passes the tick request to each hardware unit
        hardwareUnits.forEach(entry -> entry.getFirst().onTick());
    }
}
