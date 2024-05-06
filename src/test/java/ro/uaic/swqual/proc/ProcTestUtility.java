package ro.uaic.swqual.proc;

import ro.uaic.swqual.TestUtility;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;

public interface ProcTestUtility extends TestUtility {
    default Instruction add(Parameter p0, Parameter p1) {
        return new Instruction(InstructionType.ALU_ADD, p0, p1);
    }
    default Instruction sub(Parameter p0, Parameter p1) {
        return new Instruction(InstructionType.ALU_SUB, p0, p1);
    }

    default Instruction umul(Parameter p0, Parameter p1) {
        return new Instruction(InstructionType.ALU_UMUL, p0, p1);
    }

    default Instruction jmp(int idx) {
        return new Instruction(InstructionType.IPU_JMP, new Constant((char) idx));
    }

    default Instruction jeq(int idx) {
        return new Instruction(InstructionType.IPU_JEQ, new Constant((char) idx));
    }

    default Instruction jne(int idx) {
        return new Instruction(InstructionType.IPU_JNE, new Constant((char) idx));
    }

    default Instruction jgt(int idx) {
        return new Instruction(InstructionType.IPU_JGT, new Constant((char) idx));
    }

    default Instruction jge(int idx) {
        return new Instruction(InstructionType.IPU_JGE, new Constant((char) idx));
    }

    default Instruction jlt(int idx) {
        return new Instruction(InstructionType.IPU_JLT, new Constant((char) idx));
    }

    default Instruction jle(int idx) {
        return new Instruction(InstructionType.IPU_JLE, new Constant((char) idx));
    }

    default Instruction cmp(Parameter p1, Parameter p2) {
        return new Instruction(InstructionType.ALU_CMP, p1, p2);
    }

    default Constant _const(int value) { return new Constant((char) value); }

    default Register reg(int value) {
        var r = new Register();
        r.setValue((char) value);
        return r;
    }

    default Register reg() {
        return new Register();
    }

    interface FlagTestPredicate {
        boolean test(char... flags);
    }

    class TestRegister extends Register {
        public TestRegister(int value) throws ValueException {
            setValue(value);
        }
    }
}
