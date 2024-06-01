package ro.uaic.swqual.model;

import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;

import java.util.Objects;

/**
 * Represents a decoded instruction resulted from parsing an assembly instruction.
 * Can also represent instructions not meant to be executed by the processing unit
 * (i.e. still having a {@link ro.uaic.swqual.model.operands.RegisterReference RegisterReference} parameter).
 * Instructions can have up to two parameters.
 */
public class Instruction {
    /** Current instruction type */
    private InstructionType type;
    /** First parameter */
    private Parameter param1;
    /** Second parameter */
    private Parameter param2;

    /**
     * Default constructor. Does not explicitly set any values beyond the default nulls.
     */
    public Instruction() {}

    /**
     * Constructor from the instruction type
     * @param type type of instruction to be set
     */
    public Instruction(InstructionType type) {
        this.type = type;
    }

    /**
     * Constructor using type and two non-null parameters
     * @param type type of instruction to be set
     * @param param1 reference to the first parameter
     * @param param2 reference to the second parameter
     */
    public Instruction(InstructionType type, Parameter param1, Parameter param2) {
        assert param1 != null;
        assert param2 != null;
        this.type = type;
        this.param1 = param1;
        this.param2 = param2;
    }

    /**
     * Constructor using type and a single parameter
     * @param type type of instruction to be set
     * @param param1 reference to the first and only parameter
     */
    public Instruction(InstructionType type, Parameter param1) {
        assert param1 != null;
        this.type = type;
        this.param1 = param1;
    }

    /**
     * Instruction Type getter
     * @return current instruction type
     */
    public InstructionType getType() {
        return type;
    }

    /**
     * Instruction Type setter
     * @param type instruction type to be set
     */
    public void setType(InstructionType type) {
        this.type = type;
    }

    /**
     * First parameter getter
     * @return reference to the first parameter. If none exists, null.
     */
    public Parameter getParam1() {
        return param1;
    }

    /**
     * First parameter setter
     * @param param1 reference to be set as the first parameter.
     */
    public void setParam1(Parameter param1) {
        this.param1 = param1;
    }

    /**
     * Second parameter getter
     * @return reference to the second parameter. If none exists, null.
     */
    public Parameter getParam2() {
        return param2;
    }

    /**
     * Second parameter setter
     * @param param2 reference to be set as the second parameter.
     */
    public void setParam2(Parameter param2) {
        this.param2 = param2;
    }

    /**
     * Setter for both parameters.
     * @param parameters Tuple containing new first and second parameter references.
     */
    public void setParameters(Tuple2<Parameter, Parameter> parameters) {
        assert parameters != null;
        param1 = parameters.getFirst();
        param2 = parameters.getSecond();
    }

    /**
     * Getter for both parameters
     * @return tuple containing references to the first and second parameters.
     */
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
}
