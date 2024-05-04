package ro.uaic.swqual;

import ro.uaic.swqual.exception.InstructionError;
import ro.uaic.swqual.exception.ParameterError;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.Parameter;

import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

import static ro.uaic.swqual.model.operands.FlagRegister.*;

public class ALU {
    FlagRegister flagRegister;
    Register additionalOutputRegister;

    public ALU(FlagRegister flagRegister, Register additionalOutputRegister) {
        this.flagRegister = flagRegister;
        this.additionalOutputRegister = additionalOutputRegister;
    }

    public static int regRead(Parameter value) {
        var first15 = value.getValue() & 0x7FFF;
        var negative = (value.getValue() & 0x8000) != 0;
        return first15 | (negative ? 0xFFFF8000 : 0x00000000);
    }

    public static void regStore(Parameter to, int value) throws ParameterError {
        to.setValue((short) value);
    }

    private void computeAndSetOverflow(
            IntBinaryOperator compute,
            Parameter ds1,
            Parameter s2,
            Consumer<Short> overflowConsumer
    ) throws ParameterError {
        var result = compute.applyAsInt(regRead(ds1), regRead(s2));
        regStore(ds1, result);
        overflowConsumer.accept((short)(result >> 16));

        if (result == 0) {
            flagRegister.set(ZERO_FLAG);
        } else if (result > 0xFFFF || result <= -0xFFFF) {
            flagRegister.set(OVERFLOW_FLAG);
        }
    }

    private void add(Parameter destSource1, Parameter source2) throws ParameterError {
        computeAndSetOverflow(Integer::sum, destSource1, source2, o -> {});
    }

    private void sub(Parameter destSource1, Parameter source2) throws ParameterError {
        computeAndSetOverflow((a, b) -> a - b, destSource1, source2, o -> {});
    }

    private void mul(Parameter destSource1, Parameter source2) throws ParameterError {
        computeAndSetOverflow((s1, s2) -> s1 * s2, destSource1, source2, o -> additionalOutputRegister.setValue(o));
    }

    private void div(Parameter destSource1, Parameter source2) {
        throw new UnsupportedOperationException();
    }

    private void or(Parameter destSource1, Parameter source2) {
        throw new UnsupportedOperationException();
    }

    private void and(Parameter destSource1, Parameter source2) {
        throw new UnsupportedOperationException();
    }

    private void shl(Parameter destSource1, Parameter source2) {
        throw new UnsupportedOperationException();
    }

    private void shr(Parameter destSource1, Parameter source2) {
        throw new UnsupportedOperationException();
    }

    private void not(Parameter destSource1, Parameter source2) {
        throw new UnsupportedOperationException();
    }

    private void xor(Parameter destSource1, Parameter source2) {
        throw new UnsupportedOperationException();
    }

    private void compare(Parameter destSource1, Parameter source2) {
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

    public void execute(Instruction instruction) throws InstructionError, ParameterError {
        switch (instruction.getType()) {
            case ALU_ADD -> add(instruction.getParam1(), instruction.getParam2());
            case ALU_SUB -> sub(instruction.getParam1(), instruction.getParam2());
            case ALU_MUL -> mul(instruction.getParam1(), instruction.getParam2());
            case ALU_DIV -> div(instruction.getParam1(), instruction.getParam2());
            case ALU_OR -> or(instruction.getParam1(), instruction.getParam2());
            case ALU_AND -> and(instruction.getParam1(), instruction.getParam2());
            case ALU_XOR -> xor(instruction.getParam1(), instruction.getParam2());
            case ALU_SHL -> shl(instruction.getParam1(), instruction.getParam2());
            case ALU_SHR -> shr(instruction.getParam1(), instruction.getParam2());
            case ALU_NOT -> not(instruction.getParam1(), instruction.getParam2());
            case ALU_CMP -> compare(instruction.getParam1(), instruction.getParam2());
            default -> throw new InstructionError("Invalid instruction type received in ALU: \"" + instruction + "\"");
        }
    }
}
