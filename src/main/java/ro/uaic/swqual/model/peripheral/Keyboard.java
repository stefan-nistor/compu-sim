package ro.uaic.swqual.model.peripheral;

import ro.uaic.swqual.model.operands.MemoryLocation;

import java.util.concurrent.LinkedTransferQueue;

public class Keyboard extends LinkedTransferQueue<Character> implements ReadablePeripheral {
    @Override
    public char read(MemoryLocation location) {
        return this.isEmpty() ? 0 : this.peek();
    }

    @Override
    public void onTick() {
        this.poll();
    }

    public void press(Character character) {
        this.put(character);
    }

}
