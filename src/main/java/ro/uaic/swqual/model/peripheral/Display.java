package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.MemoryLocation;

import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;

public class Display implements WriteablePeripheral {
    private final FlagRegister flagRegister;
    private final byte[] byteCharacters;

    public Display(char sizeInBytes, FlagRegister flagRegister) {
        byteCharacters = new byte[sizeInBytes];
        this.flagRegister = flagRegister;
    }

    @Override
    public void write(MemoryLocation location, char value) {
        var addr = location.getValue();
        if (addr >= byteCharacters.length) {
            flagRegister.set(SEG_FLAG);
            return;
        }

        // only the lower byte is considered.
        byteCharacters[addr] = (byte) (value & 0xFF);
    }

    @Override
    public void onTick() {
        // do nothing
    }

    public String getText() {
        var sb = new StringBuilder();
        for (var byteChar : byteCharacters) {
            if (byteChar == 0) {
                break;
            }
            sb.append(Character.valueOf((char) byteChar));
        }
        return sb.toString();
    }
}
