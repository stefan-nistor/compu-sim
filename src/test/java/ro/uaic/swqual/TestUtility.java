package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.function.ThrowingRunnable;

public interface TestUtility {
    default void exceptionLess(ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    default <T> void consume(T value) {}
}
