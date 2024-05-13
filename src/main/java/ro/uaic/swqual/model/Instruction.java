package ro.uaic.swqual.model;

import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;

import java.util.Objects;

public class Instruction {
    private InstructionType type;
    private Parameter param1;
    private Parameter param2;

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
        return type == that.type && Objects.equals(param1, that.param1) && Objects.equals(param2, that.param2);
    }

    // Overriding HashCode causes Undefined Behavior.
    // I mean the C++ Undefined Behavior, that Java is not supposed to have.
    // Let's look at the previously available HashCode
    //
    // @Override
    // public int hashCode() {
    //     return Objects.hash(type, param1, param2);
    // }
    //
    // Parameter::hashCode is implemented as default on purpose - In order to resolve
    // to Object address instead of values, required since Parameters can change and can
    // screw up HashMaps.
    //
    // InstructionType::hashCode is default to enum value - no issues there, this never changes.
    //
    // It must mean that pop r7; as an instruction has the same hashCode everywhere, since
    // r7 always resolves to the same instance (CentralProcessingUnit::dataRegisters)
    //
    // In normal run, this makes sense, and hashCodes of two different pop r7; resolve to the same value
    // But in debug and under a thread pool executor, it does not. It will also include the object
    // address in the hash.
    //
    // Beautiful.


    @Override
    public String toString() {
        return type.label + " " + param1 + " " + param2;
    }

    @Override
    public String toString() {
        return type.label + " " + param1 + " " + param2;
    }
}
