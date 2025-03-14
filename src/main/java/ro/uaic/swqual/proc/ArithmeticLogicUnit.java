package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.UnresolvedMemory;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

import static ro.uaic.swqual.model.operands.FlagRegister.ILLEGAL_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.MULTISTATE_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.SEG_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.ZERO_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.OVERFLOW_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.DIV_ZERO_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.EQUAL_FLAG;
import static ro.uaic.swqual.model.operands.FlagRegister.LESS_FLAG;

/**
 * Class defining a basic 16-bit Arithmetic Logic Unit, allowing processing of the following: <br/>
 *   - addition, subtraction, signed/unsigned multiplication, signed/unsigned division of values <br/>
 *   - bitwise and, or, xor, not of values <br/>
 *   - comparison of values <br/>
 * <br/>
 * Requirements: <br/>
 *   - A flag register to set the status outcomes of operations <br/>
 *      {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister contains explicit details for these flags} <br/>
 *   - A register to output additional information to: <br/>
 *   --- ALU_*MUL uses the extra register to store the upper 16 bits of the results in.
 *       p0 = 0x0100 * p1 = 0x0100 will result in p0 = 0x0000, additional = 0x0010 <br/>
 *   --- ALU_*DIV uses the extra register to store the remainder of the operation.
 *       p0 = 0x000B / p1 = 0x0003 will result in p0 = 0x0003, additional = 0x0002  <br/>
 */
public class ArithmeticLogicUnit extends DelegatingUnit {
    /** {@link FlagRegister} Reference to use when operation status has to be signalled */
    private final FlagRegister flagRegister;

    /** Default action when ignoring an operation's overflow result */
    private final Consumer<Character> ignoreOverflow;
    /** Default action when accepting and processing an operation's overflow result */
    private final Consumer<Character> acceptOverflow;

    /**
     * Primary constructor.
     * It will create the ignore and accept overflow actions.
     * The accept overflow action will store the overflow value it to the additionalOutputRegister.
     * @param flagRegister reference to the {@link FlagRegister} to be used for raising status and errors
     * @param additionalOutputRegister reference to the {@link Register}
     *                                 to be used in overflow storage, where applicable.
     */
    public ArithmeticLogicUnit(FlagRegister flagRegister, Register additionalOutputRegister) {
        assert flagRegister != null;
        assert additionalOutputRegister != null;
        this.flagRegister = flagRegister;
        this.ignoreOverflow = o -> {};
        this.acceptOverflow = additionalOutputRegister::setValue;
    }

    /**
     * Method used to raise an error via the retained {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister}.
     * @param value flag value to raise.
     */
    @Override public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    /**
     * Method handling the actual operation execution, separating the result from the overflow, passing it to the
     * consumer and raising the expected status flags.
     * @param compute the operation to be executed
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @param overflowConsumer the consumer of the resulted overflow
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void computeAndSetOverflow(
            IntBinaryOperator compute, Parameter destSource0, Parameter source1, Consumer<Character> overflowConsumer
    ) throws ParameterException {
        assert compute != null;
        assert overflowConsumer != null;
        assert destSource0 != null;
        final var compoundResult = compute.applyAsInt(destSource0.getValue(), source1.getValue());

        /// TL; DR - Some evil bit level hacking here
        /// To be precise in what happens, all operations are computed over signed integers
        /// This allows detecting of overflows, since we are starting from an unsigned format.
        /// Any negative operations will actually be positive, but when extracting the halves, these will
        /// result in overflow and wrap-around for the result.
        ///     Example: 0xFFFF (65535 unsigned, -1 signed) + 0xFFFF = 0x1FFFE.
        ///         Extracting last 16 bits as result: 0xFFFE (65534 signed, -2 unsigned)
        ///         Both are overflows, resulting in the overflow being 1 for both.
        ///         This is important in MUL operations
        ///
        /// An exception to the abuse of int operations is the div operations
        ///     Div stores the result in the lower 16 bits, and the remainder in the highest 16 bits
        final var result = (char)(compoundResult & 0xFFFF);
        final var overflow = (char)(compoundResult >>> 16 & 0xFFFF);
        destSource0.setValue(result);
        overflowConsumer.accept(overflow);

        // If a zero-result is obtained, set zero flag.
        if (result == 0 && overflow == 0) {
            flagRegister.set(ZERO_FLAG);
        }

        // If overflow happened, regardless of whether it is stored or not, set overflow flag.
        if (overflow != 0) {
            flagRegister.set(OVERFLOW_FLAG);
        }
    }

    /**
     * Method handling the actual operation execution, not checking for overflow, and raising the expected status flags.
     * @param compute the operation to be executed
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void computeIgnoreOverflow(
            BinaryOperator<Character> compute, Parameter destSource0, Parameter source1
    ) throws ParameterException {
        assert compute != null;
        assert destSource0 != null;
        // Operations ignoring overflow do not need the integer wrap-around logic. So we can just
        // use Character operations directly.
        destSource0.setValue(compute.apply(destSource0.getValue(), source1.getValue()));

        // If a zero-result is obtained, set zero flag.
        if (destSource0.getValue() == 0) {
            flagRegister.set(ZERO_FLAG);
        }
    }

    /**
     * Method executing the {@link InstructionType#ALU_ADD add} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void add(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        // Funnily enough, java only defined Integer::sum
        computeAndSetOverflow(Integer::sum, destSource0, source1, ignoreOverflow);
    }

    /**
     * Method executing the {@link InstructionType#ALU_SUB sub} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void sub(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        computeAndSetOverflow((s0, s1) -> s0 - s1, destSource0, source1, ignoreOverflow);
    }

    /**
     * Method executing the {@link InstructionType#ALU_UMUL umul} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void umul(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        computeAndSetOverflow((s0, s1) -> s0 * s1, destSource0, source1, acceptOverflow);
    }

    /**
     * Method executing the {@link InstructionType#ALU_SMUL smul} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void smul(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        // Since the result is computed in int, we are interested in the actual value
        // So we obtain this by converting to short. 0xFFFF to int results in 65535 in int, but converting
        // to short first, we get a -1.
        computeAndSetOverflow((s0, s1) -> (short)s0 * (short)s1, destSource0, source1, acceptOverflow);
    }

    /**
     * Additional processing method for the
     *   {@link InstructionType#ALU_UDIV udiv} and {@link InstructionType#ALU_SDIV sdiv} instructions. <br/>
     * These compute both division and modulo, and the modulo has to be stored in the overflow output.
     * @param divCompute the division operation to be computed
     * @param remainderCompute the modulo operation to be computed
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void compoundDiv(
            IntBinaryOperator divCompute, IntBinaryOperator remainderCompute,
            Parameter destSource0, Parameter source1
    ) throws ParameterException {
        assert divCompute != null;
        assert remainderCompute != null;
        assert source1 != null;
        // TL; DR - Some more evil bit level hacking here
        // We store the result in the lower 16 bits and the remainder in the higher 16.
        // We also use bitwise mask and or to ensure that java does not do any implicit integer computation
        // as it usually does.
        //
        // Reason - we do not know if we are in signed or unsigned mode.
        //          If we were to do r << 16 + d, if d is 0xFFFF (-1) and r 0x0000 (0)
        //          the bitwise result would be malformed
        //          (as it would be 0xFFFFFFFF (-1), leading to loss of bit values).
        //
        //          By using bitwise composition, we keep the integrity of the bits.
        computeAndSetOverflow(
                (s0, s1) -> ((remainderCompute.applyAsInt(s0, s1) << 16) & 0xFFFF0000)
                            | (divCompute.applyAsInt(s0, s1) & 0x0000FFFF),
                destSource0, source1, acceptOverflow
        );
    }

    /**
     * Method executing the {@link InstructionType#ALU_UDIV udiv} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void udiv(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        if (source1.getValue() == 0) {
            flagRegister.set(DIV_ZERO_FLAG);
            return;
        }

        compoundDiv((s0, s1) -> s0 / s1, (s0, s1) -> s0 % s1, destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_SDIV sdiv} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void sdiv(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        if (source1.getValue() == 0) {
            flagRegister.set(DIV_ZERO_FLAG);
            return;
        }

        // Same as for smul, we (ab)use the conversion to short to turn our unsigned values into signed values
        // while keeping the same binary representation.
        compoundDiv((s0, s1) -> (short)s0 / (short)s1, (s0, s1) -> (short)s0 % (short)s1, destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_OR or} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void or(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        computeIgnoreOverflow((s0, s1) -> (char) (s0 | s1), destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_AND and} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void and(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        computeIgnoreOverflow((s0, s1) -> (char) (s0 & s1), destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_SHL shl} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void shl(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        computeIgnoreOverflow((s0, s1) -> (char) (s0 << s1), destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_SHR shr} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void shr(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        computeIgnoreOverflow((s0, s1) -> (char) (s0 >>> s1), destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_XOR xor} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void xor(Parameter destSource0, Parameter source1) throws ParameterException {
        assert source1 != null;
        computeIgnoreOverflow((s0, s1) -> (char) (s0 ^ s1), destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_NOT not} instruction.
     * @param destSource0 the first parameter and destination
     * @param source1 the second parameter, unused
     * @throws ParameterException when given non-writeable destination
     *   (e.g. {@link ro.uaic.swqual.model.operands.Constant Constant}),
     *   or when any source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void not(Parameter destSource0, Parameter source1) throws ParameterException {
        computeIgnoreOverflow((s0, s1) -> (char) ~s0, destSource0, source1);
    }

    /**
     * Method executing the {@link InstructionType#ALU_CMP cmp} instruction. Will output the results in the status
     * flag register.
     * @param source0 the first parameter
     * @param source1 the second parameter
     * @throws ParameterException when given a source is not readable
     *   (e.g. {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}).
     */
    private void compare(Parameter source0, Parameter source1) {
        assert source0 != null;
        assert source1 != null;
        final var s0 = source0.getValue();
        final var s1 = source1.getValue();

        // Evaluation logic of comparison:
        //   - s0 == s1  <==>  EQ == 1
        //   - s0 != s1  <==>  EQ == 0
        //   - s0 > s1   <==>  EQ == 0 && LT == 0
        //   - s0 < s1   <==>  EQ == 0 && LT == 1
        //   - s0 <= s1  <==>  EQ == 1 || LT == 1
        //   - s0 >= s1  <==>  EQ == 1 || LT == 0
        if (s0 == s1) {
            flagRegister.set(EQUAL_FLAG);
        } else if (s0 < s1) {
            flagRegister.set(LESS_FLAG);
        }
    }

    /**
     * Default filter for instructions. Accepts instructions according to {@link InstructionType#isAluInstruction}.
     * @return The filter interface in question.
     */
    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> InstructionType.isAluInstruction(instruction.getType());
    }

    /**
     * Method used to execute a given instruction.
     * @param instruction instruction to execute.
     * @throws InstructionException when given instruction cannot or should not be processed by
     *   the current {@link ProcessingUnit}
     * @throws ParameterException when given instruction contains any invalid/incompatible
     *   {@link ro.uaic.swqual.model.operands.Parameter Parameter} values, such as
     *   {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference}
     */
    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        assert instruction != null;
        // ensure that MemoryLocation parameters resolve to values (ResolvedMemory)
        var p0 = locate(instruction.getParam1());
        var p1 = locate(instruction.getParam2());
        assert !(p0 instanceof UnresolvedMemory);
        assert !(p1 instanceof UnresolvedMemory);

        // route parameter to requested instruction
        switch (instruction.getType()) {
            case ALU_ADD -> add(p0, p1);
            case ALU_SUB -> sub(p0, p1);
            case ALU_UMUL -> umul(p0, p1);
            case ALU_SMUL -> smul(p0, p1);
            case ALU_UDIV -> udiv(p0, p1);
            case ALU_SDIV -> sdiv(p0, p1);
            case ALU_OR -> or(p0, p1);
            case ALU_AND -> and(p0, p1);
            case ALU_XOR -> xor(p0, p1);
            case ALU_SHL -> shl(p0, p1);
            case ALU_SHR -> shr(p0, p1);
            case ALU_NOT -> not(p0, p1);
            case ALU_CMP -> compare(p0, p1);
            default -> throw new InstructionException("Invalid instruction type received in ALU: \"" + instruction + "\"");
        }

        assert !flagRegister.isSet(ILLEGAL_FLAG)
                && !flagRegister.isSet(MULTISTATE_FLAG)
                && !flagRegister.isSet(SEG_FLAG);
    }
}
