package ro.uaic.swqual.model.operands;

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
}
