package ro.uaic.swqual.model;

import ro.uaic.swqual.InstructionType;
import ro.uaic.swqual.model.operands.Parameter;

public class Instruction {
    private InstructionType type;
    private Parameter[] parameters;

    public Instruction() {

    }

    public Instruction(InstructionType type) {
        this.type = type;

    }

    public Instruction(InstructionType type, Parameter... parameters) {
        this.type = type;
        this.parameters = parameters;
    }

    public InstructionType getType() {
        return type;
    }

    public void setType(InstructionType type) {
        this.type = type;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public void setParameters(Parameter[] parameters) {
        this.parameters = parameters;
    }
}
