package ro.uaic.swqual.model.operands;

public class UnresolvedMemory extends Parameter {
    final FlagRegister flagRegister;

    public UnresolvedMemory(FlagRegister flagRegister) {
        this.flagRegister = flagRegister;
    }

    @Override public void setValue(char value) {
        flagRegister.set(FlagRegister.SEG_FLAG);
    }

    @Override public char getValue() {
        flagRegister.set(FlagRegister.SEG_FLAG);
        return 0;
    }
}
