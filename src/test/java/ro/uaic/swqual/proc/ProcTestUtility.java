package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.function.ThrowingRunnable;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;

public class ProcTestUtility {
    Instruction add(Parameter p0, Parameter p1) {
        return new Instruction(InstructionType.ALU_ADD, p0, p1);
    }
    Instruction sub(Parameter p0, Parameter p1) {
        return new Instruction(InstructionType.ALU_SUB, p0, p1);
    }

    Instruction umul(Parameter p0, Parameter p1) {
        return new Instruction(InstructionType.ALU_UMUL, p0, p1);
    }

    Instruction jmp(int idx) {
        return new Instruction(InstructionType.IPU_JMP, new Constant((char) idx));
    }

    Instruction jeq(int idx) {
        return new Instruction(InstructionType.IPU_JEQ, new Constant((char) idx));
    }

    Instruction jne(int idx) {
        return new Instruction(InstructionType.IPU_JNE, new Constant((char) idx));
    }

    Instruction jgt(int idx) {
        return new Instruction(InstructionType.IPU_JGT, new Constant((char) idx));
    }

    Instruction jge(int idx) {
        return new Instruction(InstructionType.IPU_JGE, new Constant((char) idx));
    }

    Instruction jlt(int idx) {
        return new Instruction(InstructionType.IPU_JLT, new Constant((char) idx));
    }

    Instruction jle(int idx) {
        return new Instruction(InstructionType.IPU_JLE, new Constant((char) idx));
    }

    Instruction cmp(Parameter p1, Parameter p2) {
        return new Instruction(InstructionType.ALU_CMP, p1, p2);
    }

    Constant _const(int value) { return new Constant((char) value); }

    void exceptionLess(ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    public interface FlagTestPredicate {
        boolean test(char... flags);
    }

    static class TestRegister extends Register {
        public TestRegister(int value) throws ValueException {
            setValue(value);
        }
    }
}
