package ro.uaic.swqual.model.operands;

public class UnresolvedMemory extends Parameter {
    final Runnable onAccess;

    public UnresolvedMemory(Runnable onAccess) {
        this.onAccess = onAccess;
    }

    @Override public void setValue(char value) {
        onAccess.run();
    }

    @Override public char getValue() {
        onAccess.run();
        return 0;
    }
}
