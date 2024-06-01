package ro.uaic.swqual.mem;

import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.MemoryLocation;

/*
 * Implementation detail - Little Endian (LE) Random Access Memory
 *   We use the little endian byte format, which states that larger-than-1byte values
 *     will be read least-to-most significant byte.
 *     What this means, is that the 2 byte value: [B1.B0],
 *     where B1 is the higher byte and B0 the lower byte,
 *     will be represented in memory as ... BX BX B0 B1 BX BX ...
 *   If we have a memory layout as follows:
 *   Values:      5  8  1  4  2  1  9  1  2  5
 *   Addresses:   10 11 12 13 14 15 16 17 18 19
 *
 *   If we read a 2-byte value from address 15
 *   The value will be: 1 * 256 + 9 -> 265
 *
 *   If we read a 2-byte value rom 13
 *   The value will be: 4 * 256 + 2 -> 1026
 *
 *   Pros of LE:
 *      - Equivalent to x86-64 (almost all intel/amd cpus)
 *      - Address mistakes are more obvious, since values are "reversed" as one might think
 *      - Type mistakes are more obvious, since a read of 2-byte from address X will not get the
 *        same value as reading a 4-byte value.
 *   Cons of LE:
 *      - Confusing if not layout is unknown.
 */

/**
 * Represents a unit of RAM, providing read-write access to multiple byte values, accessed by address.
 */
public class RandomAccessMemory implements ReadableWriteableMemoryUnit {
    /** Actual memory contents */
    final byte[] bytes;
    /** Reference to the {@link FlagRegister} to raise errors to */
    final FlagRegister flagRegister;

    /**
     * Constructor with implicit integer RAM size. Can be given an invalid size, in which case, it will throw
     * @param sizeInBytes amount of memory the unit will have
     * @param flagRegister reference to the {@link FlagRegister} to be used for raising status and errors
     * @throws ValueException when given a size that would generate a non-addressable space.
     * All addresses must be accessible via 16 bit values, so if given a greater than 16 bit max value, it will
     * not be addressable.
     */
    public RandomAccessMemory(int sizeInBytes, FlagRegister flagRegister) throws ValueException {
        assert flagRegister != null;

        if (sizeInBytes < 2 || sizeInBytes > Character.MAX_VALUE + 1) {
            throw new ValueException("Unaddressable memory size provided: '" + sizeInBytes + "'. "
                    + "Required size: [2, 65536] byte");
        }
        this.bytes = new byte[sizeInBytes];
        this.flagRegister = flagRegister;
    }

    /**
     * Constructor with explicitly-bounded RAM size. Cannot be given an invalid size.
     * @param sizeInBytes amount of memory the unit will have
     * @param flagRegister reference to the {@link FlagRegister} to be used for raising status and errors
     */
    public RandomAccessMemory(char sizeInBytes, FlagRegister flagRegister) {
        this.bytes = new byte[sizeInBytes];
        this.flagRegister = flagRegister;
    }

    /**
     * Method used to read a value located at a given address.
     * Will read two consecutive bytes from requested address, location and location + 1 respectively.
     * If reading out-of-range locations, the error will be signalled via setting the {@link FlagRegister#SEG_FLAG} in
     * the {@link FlagRegister} received at construction
     * @param location address of the value to read.
     * @return read value.
     */
    @Override
    public char read(MemoryLocation location) {
        assert location != null;
        var address = location.getValue();
        if (address + 1 >= bytes.length) {
            flagRegister.set(FlagRegister.SEG_FLAG);
            return 0;
        }
        // We read LE value from address. Lowest byte comes first
        // For brevity's sake, also use bit-masking to avoid any java implicit bit conversions.
        // RAM:   B0 B1
        //           Take B1 and shift it in front.
        // Value: B1 B0
        var b0 = (char)(bytes[address] & 0xFF);
        var b1 = (char)(bytes[address + 1] & 0xFF);
        return (char)(b0 + (char)(b1 << 8));
    }

    /**
     * Method used to write a value at a given address.
     * Will write the value in two consecutive bytes at the requested address, location and location + 1 respectively.
     * If reading out-of-range locations, the error will be signalled via setting the {@link FlagRegister#SEG_FLAG} in
     * the {@link FlagRegister} received at construction
     * @param location address to store to.
     * @param value value to store at address.
     */
    @Override
    public void write(MemoryLocation location, char value) {
        assert location != null;
        var address = location.getValue();
        if (address + 1 >= bytes.length) {
            flagRegister.set(FlagRegister.SEG_FLAG);
            return;
        }
        // We write LE value to address. Lowest byte comes first
        // For brevity's sake, also use bit-masking to avoid any java implicit bit conversions.
        // Value: B1 B0
        //           To take B1, shift value right.
        // Value: B1 B0
        var b0 = (byte)(value & 0xFF);
        var b1 = (byte)(value >> 8 & 0xFF);
        bytes[address] = b0;
        bytes[address + 1] = b1;
    }
}
