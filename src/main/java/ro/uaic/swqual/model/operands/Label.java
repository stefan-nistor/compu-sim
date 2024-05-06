package ro.uaic.swqual.model.operands;

public class Label extends Parameter {
    private final String name;

    public Label(String label) {
        this.name = label;
    }

    public String getName() {
        return name;
    }

}
