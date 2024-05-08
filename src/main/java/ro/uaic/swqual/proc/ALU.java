package ro.uaic.swqual.proc;

import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.Parameter;

import java.util.function.*;

import static ro.uaic.swqual.model.operands.FlagRegister.*;

/**
 * Class defining a basic 16-bit Arithmetic Logic Unit, allowing processing of the following: <br/>
 *   - addition, subtraction, signed/unsigned multiplication, signed/unsigned division of values <br/>
 *   - bitwise and, or, xor, not of values <br/>
 *   - comparison of values <br/>
 * Requirements: <br/>
 *   - A flag register to set the status outcomes of operations <br/>
 *      {@link ro.uaic.swqual.model.operands.FlagRegister FlagRegister contains explicit details for these flags} <br/>
 *   - A register to output additional information to: <br/>
 *          - ALU_*MUL uses the extra register to store the upper 16 bits of the results in. <br/>
 *            p0 = 0x0100 * p1 = 0x0100 will result in p0 = 0x0000, additional = 0x0010 <br/>
 *          - ALU_*DIV uses the extra register to store the remainder of the operation. <br/>
 *            p0 = 0x000B / p1 = 0x0003 will result in p0 = 0x0003, additional = 0x0002  <br/>
 */
public class ALU implements ProcessingUnit {
    private final FlagRegister flagRegister;

    private final Consumer<Character> ignoreOverflow;
    private final Consumer<Character> acceptOverflow;

    public ALU(FlagRegister flagRegister, Register additionalOutputRegister) {
        this.flagRegister = flagRegister;
        this.ignoreOverflow = o -> {};
        this.acceptOverflow = additionalOutputRegister::setValue;
    }

    @Override public void raiseFlag(char value) {
        flagRegister.set(value);
    }

    private void computeAndSetOverflow(
            IntBinaryOperator compute, Parameter destSource0, Parameter source1, Consumer<Character> overflowConsumer
    ) throws ParameterException {
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

        if (result == 0 && overflow == 0) {
            flagRegister.set(ZERO_FLAG);
        }

        if (overflow != 0) {
            flagRegister.set(OVERFLOW_FLAG);
        }
    }

    private void computeIgnoreOverflow(
            BinaryOperator<Character> compute, Parameter destSource0, Parameter source1
    ) throws ParameterException {
        // Operations ignoring overflow do not need the integer wrap-around logic. So we can just
        // use Character operations directly.
        destSource0.setValue(compute.apply(destSource0.getValue(), source1.getValue()));
        if (destSource0.getValue() == 0) {
            flagRegister.set(ZERO_FLAG);
        }
    }

    private void add(Parameter destSource0, Parameter source1) throws ParameterException {
        // Funnily enough, java only defined Integer::sum
        computeAndSetOverflow(Integer::sum, destSource0, source1, ignoreOverflow);
    }

    private void sub(Parameter destSource0, Parameter source1) throws ParameterException {
        computeAndSetOverflow((s0, s1) -> s0 - s1, destSource0, source1, ignoreOverflow);
    }

    private void umul(Parameter destSource0, Parameter source1) throws ParameterException {
        computeAndSetOverflow((s0, s1) -> s0 * s1, destSource0, source1, acceptOverflow);
    }

    private void smul(Parameter destSource0, Parameter source1) throws ParameterException {
        // Since the result is computed in int, we are interested in the actual value
        // So we obtain this by converting to short. 0xFFFF to int results in 65535 in int, but converting
        // to short first, we get a -1.
        computeAndSetOverflow((s0, s1) -> (short)s0 * (short)s1, destSource0, source1, acceptOverflow);
    }

    private void compoundDiv(
            IntBinaryOperator divCompute, IntBinaryOperator remainderCompute,
            Parameter destSource0, Parameter source1
    ) throws ParameterException {
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

    private void udiv(Parameter destSource0, Parameter source1) throws ParameterException {
        if (source1.getValue() == 0) {
            flagRegister.set(DIV_ZERO_FLAG);
            return;
        }

        compoundDiv((s0, s1) -> s0 / s1, (s0, s1) -> s0 % s1, destSource0, source1);
    }

    private void sdiv(Parameter destSource0, Parameter source1) throws ParameterException {
        if (source1.getValue() == 0) {
            flagRegister.set(DIV_ZERO_FLAG);
            return;
        }

        // Same as for smul, we (ab)use the conversion to short to turn our unsigned values into signed values
        // while keeping the same binary representation.
        compoundDiv((s0, s1) -> (short)s0 / (short)s1, (s0, s1) -> (short)s0 % (short)s1, destSource0, source1);
    }

    private void or(Parameter destSource0, Parameter source1) throws ParameterException {
        computeIgnoreOverflow((s0, s1) -> (char) (s0 | s1), destSource0, source1);
    }

    private void and(Parameter destSource0, Parameter source1) throws ParameterException {
        computeIgnoreOverflow((s0, s1) -> (char) (s0 & s1), destSource0, source1);
    }

    private void shl(Parameter destSource0, Parameter source1) throws ParameterException {
        computeIgnoreOverflow((s0, s1) -> (char) (s0 << s1), destSource0, source1);
    }

    private void shr(Parameter destSource0, Parameter source1) throws ParameterException {
        computeIgnoreOverflow((s0, s1) -> (char) (s0 >>> s1), destSource0, source1);
    }

    private void not(Parameter destSource0, Parameter source1) throws ParameterException {
        computeIgnoreOverflow((s0, s1) -> (char) ~s0, destSource0, source1);
    }

    private void xor(Parameter destSource0, Parameter source1) throws ParameterException {
        computeIgnoreOverflow((s0, s1) -> (char) (s0 ^ s1), destSource0, source1);
    }

    private void compare(Parameter destSource0, Parameter source1) {
        final var s0 = destSource0.getValue();
        final var s1 = source1.getValue();

        /*
         * Evaluation logic of comparison:
         * s0 == s1  <==>  EQ == 1
         * s0 != s1  <==>  EQ == 0
         * s0 > s1   <==>  EQ == 0 && LT == 0
         * s0 < s1   <==>  EQ == 0 && LT == 1
         * s0 <= s1  <==>  EQ == 1 || LT == 1
         * s0 >= s1  <==>  EQ == 1 || LT == 0
         */
        if (s0 == s1) {
            flagRegister.set(EQUAL_FLAG);
        } else if (s0 < s1) {
            flagRegister.set(LESS_FLAG);
        }
    }

    @Override
    public Predicate<Instruction> getDefaultFilter() {
        return instruction -> instruction.getType().ordinal() >= InstructionType.ALU_ADD.ordinal()
                           && instruction.getType().ordinal() <= InstructionType.ALU_CMP.ordinal();
    }

    @Override
    public void execute(Instruction instruction) throws InstructionException, ParameterException {
        switch (instruction.getType()) {
            case ALU_ADD -> add(instruction.getParam1(), instruction.getParam2());
            case ALU_SUB -> sub(instruction.getParam1(), instruction.getParam2());
            case ALU_UMUL -> umul(instruction.getParam1(), instruction.getParam2());
            case ALU_SMUL -> smul(instruction.getParam1(), instruction.getParam2());
            case ALU_UDIV -> udiv(instruction.getParam1(), instruction.getParam2());
            case ALU_SDIV -> sdiv(instruction.getParam1(), instruction.getParam2());
            case ALU_OR -> or(instruction.getParam1(), instruction.getParam2());
            case ALU_AND -> and(instruction.getParam1(), instruction.getParam2());
            case ALU_XOR -> xor(instruction.getParam1(), instruction.getParam2());
            case ALU_SHL -> shl(instruction.getParam1(), instruction.getParam2());
            case ALU_SHR -> shr(instruction.getParam1(), instruction.getParam2());
            case ALU_NOT -> not(instruction.getParam1(), instruction.getParam2());
            case ALU_CMP -> compare(instruction.getParam1(), instruction.getParam2());
            default -> throw new InstructionException("Invalid instruction type received in ALU: \"" + instruction + "\"");
        }
    }
}
