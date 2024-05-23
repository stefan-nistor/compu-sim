package ro.uaic.swqual.tester;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class PredicateExpectation extends Expectation {
    private Predicate<String> callback;
    private Supplier<String> dumpHintSupplier;
    private String data;

    public void setCallback(Predicate<String> callback) {
        assert callback != null;
        this.callback = callback;
    }

    public void setDumpHintSupplier(Supplier<String> dumpHintSupplier) {
        assert dumpHintSupplier != null;
        this.dumpHintSupplier = dumpHintSupplier;
    }

    @Override
    public boolean evaluate() {
        if (callback == null) {
            return false;
        }

        return callback.test(data);
    }

    @Override
    protected void load(String data) {
        this.data = data;
    }

    @Override
    public String dump() {
        if (callback == null) {
            return "\tFailed due to lack of callback. tag='" + tag + "'";
        }

        String dumpHint = "";
        if (dumpHintSupplier != null) {
            dumpHint = ", expected/hint = " + dumpHintSupplier.get();
        }

        return "\tFailed due to callback -> PredicateExpectation {tag = '" + tag + "', data='" + data + "'" + dumpHint + "}";
    }
}
