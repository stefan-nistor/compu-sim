package ro.uaic.swqual.unit;

import org.junit.jupiter.api.Assertions;

public interface TestUtility {
    interface ThrowingRunnable {
        void run() throws Throwable;
    }

    default void exceptionLess(ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            Assertions.fail(t.getMessage());
        }
    }

    default <T> void discard(T value) {}
}
