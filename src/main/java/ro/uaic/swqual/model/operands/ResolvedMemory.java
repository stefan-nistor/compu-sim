package ro.uaic.swqual.model.operands;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a resolved value in any non-concrete, identified memory location. <br/>
 * <br/>
 * Obtained from: <br/>
 *  - {@link ro.uaic.swqual.proc.LocatingUnit#locate LocatingUnit.locate} <br/>
 *  - or {@link ro.uaic.swqual.proc.ProxyUnit#locate ProxyUnit.locate} <br/>
 * when given a {@link MemoryLocation} parameter that addresses a location in an owned: <br/>
 *  - {@link ro.uaic.swqual.mem.MemoryUnit MemoryUnit} <br/>
 *  - or {@link ro.uaic.swqual.model.peripheral.Peripheral Peripheral} <br/>
 * <br/>
 * Uses: <br/>
 *  - {@link Supplier} to be a proxy for: <br/>
 *  --- {@link ro.uaic.swqual.mem.ReadableMemoryUnit#read ReadableMemoryUnit.read} <br/>
 *  --- or {@link ro.uaic.swqual.model.peripheral.ReadablePeripheral#read ReadablePeripheral.read} <br/>
 *  - and {@link Consumer} to be a proxy for writing values in: <br/>
 *  --- {@link ro.uaic.swqual.mem.WriteableMemoryUnit#write WriteableMemoryUnit.write} <br/>
 *  --- or {@link ro.uaic.swqual.model.peripheral.WriteablePeripheral#write WriteablePeripheral.write}
 */
public class ResolvedMemory extends Parameter {
    /** Read proxy */
    private final Supplier<Character> getProxy;
    /** Write proxy */
    private final Consumer<Character> setProxy;

    /**
     * Primary constructor
     * @param getProxy read proxy, can be null (if no read access is done)
     * @param setProxy write proxy, can be null (if no write access is done)
     */
    public ResolvedMemory(Supplier<Character> getProxy, Consumer<Character> setProxy) {
        this.getProxy = getProxy;
        this.setProxy = setProxy;
    }

    /**
     * Method used to pass set to proxy, writing value in resolved location.
     * If called from non-writeable resolved memory, will intentionally crash.
     * @param value value intended to be stored via proxy
     */
    @Override
    public void setValue(char value) {
        assert setProxy != null;
        setProxy.accept(value);
    }


    /**
     * Method used to request read via proxy.
     * If called from non-readable resolved memory, will intentionally crash.
     * @return read value from resolved memory
     */
    @Override
    public char getValue() {
        assert getProxy != null;
        var memory = getProxy.get();
        assert memory != null;
        return memory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedMemory that = (ResolvedMemory) o;
        return Objects.equals(getProxy, that.getProxy) && Objects.equals(setProxy, that.setProxy);
    }

    @Override
    public String toString() {
        return "mem(" + (int) getValue() + ")";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
