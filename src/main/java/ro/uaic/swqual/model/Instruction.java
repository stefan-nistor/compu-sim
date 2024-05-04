package ro.uaic.swqual.model;

import ro.uaic.swqual.InstructionType;
import ro.uaic.swqual.model.operands.Parameter;

import java.util.ArrayList;
import java.util.List;

public class Instruction {
    private InstructionType type;
    private Parameter param1;
    private Parameter param2;

    public Instruction() {

    }

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

    public void setParameters(List<Parameter> parameterList) {
        this.param1 = parameterList.getFirst();
        this.param2 = parameterList.getLast();
    }

    public List<Parameter> getParameters() {
        var result = new ArrayList<Parameter>();
        result.add(this.param1);
        result.add(this.param2);
        return result;
    }
}
