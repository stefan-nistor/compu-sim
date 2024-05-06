package ro.uaic.swqual.proc;

import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.peripheral.Peripheral;

/**
 * I/O unit
 */
public class InputOutputManagementUnit extends ProxyUnit<Peripheral> implements ClockDependent {
    private final FlagRegister flagRegister;

    public InputOutputManagementUnit(FlagRegister flagRegister) {
        this.flagRegister = flagRegister;
    }

    @Override
    public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    @Override
    public void onTick() {
        hardwareUnits.forEach(entry -> entry.getFirst().onTick());
    }
}
