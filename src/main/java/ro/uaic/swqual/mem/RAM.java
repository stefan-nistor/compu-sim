package ro.uaic.swqual.mem;

import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.Register;

public class RAM implements MemoryUnit {
    final byte[] bytes;
    final Register flagRegister;

    public RAM(int sizeInBytes, Register flagRegister) throws ValueException {
        if (sizeInBytes < 2 || sizeInBytes > Character.MAX_VALUE + 1) {
            throw new ValueException("Unaddressable memory size provided: '" + sizeInBytes + "'. "
                    + "Required size: [2, 65536] byte");
        }
        this.bytes = new byte[sizeInBytes];
        this.flagRegister = flagRegister;
    }

    @Override
    public char read(MemoryLocation location) {
        var address = location.getValue();
        if (address + 1 >= bytes.length) {
            flagRegister.setValue(FlagRegister.SEG_FLAG);
            return 0;
        }
        var b0 = (char)(bytes[address] & 0xFF);
        var b1 = (char)(bytes[address + 1] & 0xFF);
        return (char)(b0 + (char)(b1 << 8));
    }

    @Override
    public void write(MemoryLocation location, char value) {
        var address = location.getValue();
        if (address + 1 >= bytes.length) {
            flagRegister.setValue(FlagRegister.SEG_FLAG);
            return;
        }
        var b0 = (byte)(value & 0xFF);
        var b1 = (byte)(value >> 8 & 0xFF);
        bytes[address] = b0;
        bytes[address + 1] = b1;
    }
}
