package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.MemoryLocation;

import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;

/**
 * Represents an ASCII text display. Provides write-only access to the bytes representing the characters written on it.
 * It will store the character values inside a byte array, allowing a byte for each character. The length of the
 * string is determined via a terminating null character.
 */
public class Display implements WriteablePeripheral {
    /** Reference to the {@link FlagRegister} to report invalid accesses to */
    private final FlagRegister flagRegister;
    /** Character byte-array */
    private final byte[] byteCharacters;

    /**
     * Constructor with explicitly-bounded size. Cannot be given an invalid size.
     * @param sizeInBytes number of characters the display will have (implicitly, also the number of bytes in its memory)
     * @param flagRegister reference to the {@link FlagRegister} to be used for raising status and errors
     */
    public Display(char sizeInBytes, FlagRegister flagRegister) {
        assert flagRegister != null;
        byteCharacters = new byte[sizeInBytes];
        this.flagRegister = flagRegister;
    }

    /**
     * Method used to write a value at a given address to be displayed.
     * Will only use the lower byte of the given value to extract a character value from. It will always store it
     * to the given location.
     * If reading out-of-range locations, the error will be signalled via setting the {@link FlagRegister#SEG_FLAG} in
     * the {@link FlagRegister} received at construction
     * @param location address to store to.
     * @param value value to store at address.
     */
    @Override
    public void write(MemoryLocation location, char value) {
        assert location != null;
        var addr = location.getValue();
        if (addr >= byteCharacters.length) {
            flagRegister.set(SEG_FLAG);
            return;
        }

        // only the lower byte is considered.
        byteCharacters[addr] = (byte) (value & 0xFF);
    }

    /**
     * Method used to synchronize peripheral to the clock. Unused in this case
     */
    @Override
    public void onTick() {
        // do nothing
    }

    /**
     * Getter for actual displayed text. Should be used outside any processing unit.
     * @return Displayed contents.
     */
    public String getText() {
        // single byte characters have to be converted to actual java char values.
        var sb = new StringBuilder();
        for (var byteChar : byteCharacters) {
            // Stop at null terminator.
            if (byteChar == 0) {
                break;
            }
            sb.append(Character.valueOf((char) byteChar));
        }
        return sb.toString();
    }
}
