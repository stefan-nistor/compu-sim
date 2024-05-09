package ro.uaic.swqual.model;

import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;

import java.util.Objects;

public class Instruction {
    private InstructionType type = null;
    private Parameter param1 = null;
    private Parameter param2 = null;

    public Instruction() {}

    public Instruction(InstructionType type) {
        this.type = type;
    }

    public Instruction(InstructionType type, Parameter param1, Parameter param2) {
        this.type = type;
        this.param1 = param1;
        this.param2 = param2;
    }

    public Instruction(InstructionType type, Parameter param1) {
        this.type = type;
        this.param1 = param1;
    }

    public InstructionType getType() {
        return type;
    }

    public void setType(InstructionType type) {
        this.type = type;
    }

    public Parameter getParam1() {
        return param1;
    }

    public void setParam1(Parameter param1) {
        this.param1 = param1;
    }

    public Parameter getParam2() {
        return param2;
    }

    public void setParam2(Parameter param2) {
        this.param2 = param2;
    }

    public void setParameters(Tuple2<Parameter, Parameter> parameters) {
        param1 = parameters.getFirst();
        param2 = parameters.getSecond();
    }

    public Tuple2<Parameter, Parameter> getParameters() {
        return Tuple.of(param1, param2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instruction that = (Instruction) o;
        // Not worth implementing equals for every single operand type, as they should only be compared in tests.
        // By architectural design, equality of Parameter values should never be done by Object.equals, but by
        // Parameter::getValue, since the behavior is different depending on cases.
        return type == that.type
            && param1.getClass() == that.param1.getClass()
            && param2.getClass() == that.param2.getClass()
            && param1.getValue() == that.param1.getValue()
            && param2.getValue() == that.param2.getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, param1, param2);
    }
}
