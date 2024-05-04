package ro.uaic.swqual;

import ro.uaic.swqual.exception.InstructionError;

public class ALU {
    Register flagRegister;

    public ALU(Register flagRegister) {
        this.flagRegister = flagRegister;
    }

    private void add(Register destSource1, Register source2) {}

    private void sub(Register destSource1, Register source2) {}

    private void mul(Register destSource1, Register source2) {}

    private void div(Register destSource1, Register source2) {}

    private void mod(Register destSource1, Register source2) {}

    private void greaterThan(Register destSource1, Register source2) {}

    private void lessThan(Register destSource1, Register source2) {}

    private void greaterEqual(Register destSource1, Register source2) {}

    private void lessEqual(Register destSource1, Register source2) {}

    private void equal(Register destSource1, Register source2) {}

    private void notEqual(Register destSource1, Register source2) {}

    public void execute(InstructionType instruction, Register destSource1, Register source2) throws InstructionError {
        switch (instruction) {
            case ALU_ADD -> add(destSource1, source2);
            case ALU_SUB -> sub(destSource1, source2);
            case ALU_MUL -> mul(destSource1, source2);
            case ALU_DIV -> div(destSource1, source2);
            case ALU_MOD -> mod(destSource1, source2);
            case ALU_GT -> greaterThan(destSource1, source2);
            case ALU_LT -> lessThan(destSource1, source2);
            case ALU_GE -> greaterEqual(destSource1, source2);
            case ALU_LE -> lessEqual(destSource1, source2);
            case ALU_EQ -> equal(destSource1, source2);
            case ALU_NE -> notEqual(destSource1, source2);
        }

        throw new InstructionError("Invalid instruction type received in ALU: \"" + instruction + "\"");
    }
}
