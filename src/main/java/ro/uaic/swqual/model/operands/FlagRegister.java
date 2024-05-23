package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;

import java.util.stream.Stream;

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
    public static final char OVERFLOW_FLAG      = 0x0001;

    /**
     * ZERO_FLAG is set when:
     *      - ALU_* results in a pure zero result
     *          A pure zero result means that all output parameters are zero.
     *          Even if an overflow happens and the primary output (param0) is 0,
     *          this is not considered a zero-result. (ex. umul 0x0100, 0x0100)
     */
    public static final char ZERO_FLAG          = 0x0002;

    /**
     * DIV_ZERO_FLAG is set when ALU_*DIV is invoked with the second param's evaluated value as 0
     */
    public static final char DIV_ZERO_FLAG      = 0x0004;

    /**
     * EQUAL_FLAG is set when ALU_CMP is invoked with equally evaluated parameters
     */
    public static final char EQUAL_FLAG         = 0x0008;

    /**
     * LESS_FLAG is set when ALU_CMP is invoked with the first parameter evaluated as less than the second
     */
    public static final char LESS_FLAG          = 0x0010;

    /**
     * ILLEGAL_FLAG is set when the IPU attempts to execute an illegal instruction
     */
    public static final char ILLEGAL_FLAG       = 0x0020;

    /**
     * SEG_FLAG is set when the MMU attempts access to an invalid memory location
     */
    public static final char SEG_FLAG           = 0x0040;

    /**
     * MULTISTATE_FLAG is set when the MMU receives a request to locate an address that resolves to more than one
     *      memory space. Example: if I/O accepts addressed 0x20, 0x40, and RAM 0x40, 0x60, Addressing 0x40 causes
     *      a multistate.
     */
    public static final char MULTISTATE_FLAG    = 0x0080;

    private static final char BITMASK =
            OVERFLOW_FLAG | ZERO_FLAG | DIV_ZERO_FLAG | EQUAL_FLAG | LESS_FLAG | ILLEGAL_FLAG | SEG_FLAG
                    | MULTISTATE_FLAG;

    private void stateValidation() {
        assert value == (value & BITMASK);
    }

    public void clear() {
        setValue((char)0);
    }

    public void set(char flag) {
        setValue((char)(getValue() | flag));
        stateValidation();
    }

    public void unset(char flag) {
        setValue((char)(getValue() & ~flag));
        stateValidation();
    }

    public boolean isSet(char flag) {
        return (getValue() & flag) != 0;
    }

    @Override
    public String toString() {
        return Stream.of(
                Tuple.of(OVERFLOW_FLAG, "OVFL"),
                Tuple.of(ZERO_FLAG, "ZERO"),
                Tuple.of(DIV_ZERO_FLAG, "DZERO"),
                Tuple.of(EQUAL_FLAG, "EQ"),
                Tuple.of(LESS_FLAG, "LE"),
                Tuple.of(ILLEGAL_FLAG, "ILL"),
                Tuple.of(SEG_FLAG, "SEG"),
                Tuple.of(MULTISTATE_FLAG, "MST")
        ).filter(e -> isSet(e.getFirst())).map(Tuple2::getSecond)
                .reduce((l, r) -> l + ", " + r).orElse("");
    }
}
