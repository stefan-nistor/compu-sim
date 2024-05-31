package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.model.operands.MemoryLocation;

import java.util.concurrent.LinkedTransferQueue;

/**
 * Represents a keyboard, acting as a FIFO of characters.
 * It will store each character via calls to {@link Keyboard#press} from outside the processing units, to be accessed
 * later via {@link Keyboard#read} later. Each read call pops from the queue.
 */
public class Keyboard extends LinkedTransferQueue<Character> implements ReadablePeripheral {
    /**
     * Method used to read the character at the front of the queue.
     * @param location unused, present for interface reasons. Keyboard is a single-address peripheral.
     * @return current character. 0 if no key was present in the queue.
     */
    @Override
    public char read(MemoryLocation location) {
        assert location != null;
        if (isEmpty()) {
            return 0;
        }

        return this.poll();
    }

    /**
     * Method used to synchronize peripheral to the clock. Unused in this case
     */
    @Override
    public void onTick() {
        // do nothing
    }

    /**
     * Method used to push a character to the queue. Should be used outside any processing unit.
     * @param character value to be pushed.
     */
    public void press(Character character) {
        assert character != null;
        this.put(character);
    }
}
