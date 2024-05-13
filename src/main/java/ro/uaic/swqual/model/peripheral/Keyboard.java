package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.model.operands.MemoryLocation;

import java.util.concurrent.LinkedTransferQueue;

public class Keyboard extends LinkedTransferQueue<Character> implements ReadablePeripheral {
    @Override
    public char read(MemoryLocation location) {
        if (isEmpty()) {
            return 0;
        }

        return this.poll();
    }

    @Override
    public void onTick() {
        // do nothing
    }

    public void press(Character character) {
        this.put(character);
    }
}
