package ro.uaic.swqual.model.operands;

public class FlagRegister extends Register {
    /**
     * OVERFLOW_FLAG is set when:
     *      - ALU_ADD overflows (0xFFF0 + 0x00FF)
     *      - ALU_SUB overflows (0x0010 - 0x0FFF)
     *      - ALU_UMUL overflows (0x0100 * 0x0100). additionalOutputRegister will
     *          contain the extra 16 bits. To get the fullResult, (addOut << 16) + out.
     *          This might not be always obvious, but when this happens, the full result
     *          should be taken into account
     *      - ALU_SMUL -> same as ALU_UMUL.
     *      - ALU_DIV outputs a remainder that is not 0. Reg1 (first param) will contain
     *          the result, additionalOutputRegister will contain the register.
     */
    public static final char OVERFLOW_FLAG = 0x0001;

    /**
     * ZERO_FLAG is set when:
     *      - ALU_* results in a pure zero result
     *          A pure zero result means that all output parameters are zero.
     *          Even if an overflow happens and the primary output (param0) is 0,
     *          this is not considered a zero-result. (ex. umul 0x0100, 0x0100)
     */
    public static final char ZERO_FLAG     = 0x0002;

    /**
     * DIV_ZERO_FLAG is set when ALU_*DIV is invoked with the second param's evaluated value as 0
     */
    public static final char DIV_ZERO_FLAG = 0x0004;

    /**
     * EQUAL_FLAG is set when ALU_CMP is invoked with equally evaluated parameters
     */
    public static final char EQUAL_FLAG    = 0x0008;

    /**
     * LESS_FLAG is set when ALU_CMP is invoked with the first parameter evaluated as less than the second
     */
    public static final char LESS_FLAG     = 0x0010;

    public void clear() {
        setValue((char)0);
    }

    public void set(char flag) {
        setValue((char)(getValue() | flag));
    }

    public void unset(char flag) {
        setValue((char)(getValue() & ~flag));
    }

    public boolean isSet(char flag) {
        return (getValue() & flag) != 0;
    }
}
