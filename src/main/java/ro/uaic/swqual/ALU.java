package ro.uaic.swqual;

import ro.uaic.swqual.exception.InstructionError;
import ro.uaic.swqual.model.operands.Register;

import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

import static ro.uaic.swqual.FlagRegister.*;

public class ALU {
    FlagRegister flagRegister;
    Register additionalOutputRegister;

    public ALU(FlagRegister flagRegister, Register additionalOutputRegister) {
        this.flagRegister = flagRegister;
        this.additionalOutputRegister = additionalOutputRegister;
    }

    public static int regRead(Register value) {
        var first15 = value.getValue() & 0x7FFF;
        var negative = (value.getValue() & 0x8000) != 0;
        return first15 | (negative ? 0xFFFF8000 : 0x00000000);
    }

    public static void regStore(Register to, int value) {
        to.setValue((short) value);
    }

    private void computeAndSetOverflow(
            IntBinaryOperator compute,
            Register ds1,
            Register s2,
            Consumer<Short> overflowConsumer
    ) {
        var result = compute.applyAsInt(regRead(ds1), regRead(s2));
        regStore(ds1, result);
        overflowConsumer.accept((short)(result >> 16));

        if (result == 0) {
            flagRegister.set(ZERO_FLAG);
        } else if (result > 0xFFFF || result <= -0xFFFF) {
            flagRegister.set(OVERFLOW_FLAG);
        }
    }

    private void add(Register destSource1, Register source2) {
        computeAndSetOverflow(Integer::sum, destSource1, source2, o -> {});
    }

    private void sub(Register destSource1, Register source2) {
        computeAndSetOverflow((a, b) -> a - b, destSource1, source2, o -> {});
    }

    private void mul(Register destSource1, Register source2) {
        computeAndSetOverflow((s1, s2) -> s1 * s2, destSource1, source2, o -> additionalOutputRegister.setValue(o));
    }

    private void div(Register destSource1, Register source2) {
        throw new UnsupportedOperationException();
    }

    private void or(Register destSource1, Register source2) {
        throw new UnsupportedOperationException();
    }

    private void and(Register destSource1, Register source2) {
        throw new UnsupportedOperationException();
    }

    private void shl(Register destSource1, Register source2) {
        throw new UnsupportedOperationException();
    }

    private void shr(Register destSource1, Register source2) {
        throw new UnsupportedOperationException();
    }

    private void not(Register destSource1, Register source2) {
        throw new UnsupportedOperationException();
    }

    private void xor(Register destSource1, Register source2) {
        throw new UnsupportedOperationException();
    }

    private void compare(Register destSource1, Register source2) {
        var s1 = (int)destSource1.getValue();
        var s2 = (int)source2.getValue();

        // s1 == s2  <==>  EQ == 1
        // s1 != s2  <==>  EQ == 0
        // s1 > s2   <==>  EQ == 0 && LT == 0
        // s1 < s2   <==>  EQ == 0 && LT == 1
        // s1 <= s2  <==>  EQ == 1 || LT == 1
        // s1 >= s2  <==>  EQ == 1 || LT == 0
        if (s1 == s2) {
            flagRegister.set(EQUAL_FLAG);
        } else if (s1 < s2) {
            flagRegister.set(LESS_FLAG);
        }
    }

    public void execute(InstructionType instruction, Register destSource1, Register source2) throws InstructionError {
        switch (instruction) {
            case ALU_ADD -> add(destSource1, source2);
            case ALU_SUB -> sub(destSource1, source2);
            case ALU_MUL -> mul(destSource1, source2);
            case ALU_DIV -> div(destSource1, source2);
            case ALU_OR -> or(destSource1, source2);
            case ALU_AND -> and(destSource1, source2);
            case ALU_XOR -> xor(destSource1, source2);
            case ALU_SHL -> shl(destSource1, source2);
            case ALU_SHR -> shr(destSource1, source2);
            case ALU_NOT -> not(destSource1, source2);
            case ALU_CMP -> compare(destSource1, source2);
            default -> throw new InstructionError("Invalid instruction type received in ALU: \"" + instruction + "\"");
        }
    }
}
