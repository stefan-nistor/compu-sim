package ro.uaic.swqual.proc;

/**
 * Represents a unit that can be synchronized to a CPU Clock.
 */
public interface ClockListener {
    /**
     * Method to be called on each clock tick.
     */
    void onTick();
}
