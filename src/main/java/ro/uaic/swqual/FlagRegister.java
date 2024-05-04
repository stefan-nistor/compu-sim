package ro.uaic.swqual;

public class FlagRegister extends Register {
    public static final short OVERFLOW_FLAG = 0x0001;
    public static final short ZERO_FLAG     = 0x0002;
    public static final short EQUAL_FLAG    = 0x0004;
    public static final short LESS_FLAG     = 0x0008;

    public void clear() {
        setValue((short)0);
    }

    public void set(short flag) {
        setValue((short)(getValue() | flag));
    }

    public boolean isSet(short flag) {
        return (getValue() & flag) != 0;
    }
}
