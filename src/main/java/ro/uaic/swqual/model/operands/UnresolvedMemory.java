package ro.uaic.swqual.model.operands;

import java.util.Objects;

/**
 * Represents an unresolved memory location. <br/>
 * <br/>
 * Obtained from: <br/>
 *  - {@link ro.uaic.swqual.proc.LocatingUnit#locate LocatingUnit.locate} <br/>
 *  - or {@link ro.uaic.swqual.proc.ProxyUnit#locate ProxyUnit.locate} <br/>
 * when given a {@link MemoryLocation} parameter that does not address any location in any owned: <br/>
 *  - {@link ro.uaic.swqual.mem.MemoryUnit MemoryUnit} <br/>
 *  - or {@link ro.uaic.swqual.model.peripheral.Peripheral Peripheral} <br/>
 * <br/>
 * Allows reacting to invalid access via the passed {@link Runnable}
 */
public class UnresolvedMemory extends Parameter {
    /** Runnable allowing for reacting on invalid access */
    private final Runnable onAccess;

    /**
     * Primary constructor
     * @param onAccess runnable to be called on invalid read/write. Can be null (if no read/write is expected)
     */
    public UnresolvedMemory(Runnable onAccess) {
        this.onAccess = onAccess;
    }

    /**
     * Method intercepting any write to the invalid location
     * @param value unused.
     */
    @Override public void setValue(char value) {
        assert onAccess != null;
        onAccess.run();
    }

    /**
     * Method intercepting any read from the invalid location
     * @return always 0.
     */
    @Override public char getValue() {
        assert onAccess != null;
        onAccess.run();
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnresolvedMemory that = (UnresolvedMemory) o;
        return Objects.equals(onAccess, that.onAccess);
    }

    @Override
    public String toString() {
        return "<Invalid Location>";
    }

    // HashCode is intentionally NOT overridden here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
}
