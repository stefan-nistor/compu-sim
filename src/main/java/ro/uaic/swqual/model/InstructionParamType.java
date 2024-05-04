package ro.uaic.swqual.model;


public class InstructionParamType {

    private types type;
    private short value;

    public types getType() {
        return type;
    }

    public void setType(types type) {
        this.type = type;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    public enum types {
        REG, CONST, MEM
    }

}


