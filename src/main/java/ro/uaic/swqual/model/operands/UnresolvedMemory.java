package ro.uaic.swqual.model.operands;

import java.util.Objects;

public class UnresolvedMemory extends Parameter {
    private final Runnable onAccess;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UnresolvedMemory that = (UnresolvedMemory) o;
        return Objects.equals(onAccess, that.onAccess);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), onAccess);
    }
}
