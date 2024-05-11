package ro.uaic.swqual.model.operands;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ResolvedMemory extends Parameter {
    private final Supplier<Character> getProxy;
    private final Consumer<Character> setProxy;

    public ResolvedMemory(Supplier<Character> getProxy, Consumer<Character> setProxy) {
        this.getProxy = getProxy;
        this.setProxy = setProxy;
    }

    @Override
    public void setValue(char value) {
        setProxy.accept(value);
    }

    @Override
    public char getValue() {
        return getProxy.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedMemory that = (ResolvedMemory) o;
        return Objects.equals(getProxy, that.getProxy) && Objects.equals(setProxy, that.setProxy);
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
