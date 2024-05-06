package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.function.ThrowingRunnable;
import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;

public class ProcTest {
    Instruction add(Parameter p0, Parameter p1) {
        return new Instruction(InstructionType.ALU_ADD, p0, p1);
    }

    Instruction jmp(char idx) {
        return new Instruction(InstructionType.IPU_JMP, new Constant(idx));
    }

    Instruction jeq(char idx) {
        return new Instruction(InstructionType.IPU_JEQ, new Constant(idx));
    }

    Instruction jne(char idx) {
        return new Instruction(InstructionType.IPU_JNE, new Constant(idx));
    }

    Instruction jgt(char idx) {
        return new Instruction(InstructionType.IPU_JGT, new Constant(idx));
    }

    Instruction jge(char idx) {
        return new Instruction(InstructionType.IPU_JGE, new Constant(idx));
    }

    Instruction jlt(char idx) {
        return new Instruction(InstructionType.IPU_JLT, new Constant(idx));
    }

    Instruction jle(char idx) {
        return new Instruction(InstructionType.IPU_JLE, new Constant(idx));
    }

    Instruction cmp(Parameter p1, Parameter p2) {
        return new Instruction(InstructionType.ALU_CMP, p1, p2);
    }

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
