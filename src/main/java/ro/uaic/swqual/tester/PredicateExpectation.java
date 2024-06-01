package ro.uaic.swqual.tester;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents an expectation that, when evaluated, will provide the result via a given callback.
 */
public class PredicateExpectation extends Expectation {
    /** The callback that will provide the expectation data upon evaluation request */
    private Predicate<String> callback;
    /** Supplier for obtaining a hint, used in dumping, to separate PredicateExpectations from each other */
    private Supplier<String> dumpHintSupplier;
    /** The data the expectation contains */
    private String data;

    /**
     * Expectation callback setter
     * @param callback the new callback to be used at evaluation
     */
    public void setCallback(Predicate<String> callback) {
        assert callback != null;
        this.callback = callback;
    }

    /**
     * Dump Hint supplier setter
     * @param dumpHintSupplier the new supplier to be used
     */
    public void setDumpHintSupplier(Supplier<String> dumpHintSupplier) {
        assert dumpHintSupplier != null;
        this.dumpHintSupplier = dumpHintSupplier;
    }

    /**
     * Method used to invoke the expectation evaluation. Invokes callback if present.
     * @return true if expectation evaluated as-expected, false otherwise
     */
    @Override
    public boolean evaluate() {
        if (callback == null) {
            return false;
        }

        return callback.test(data);
    }

    /**
     * Method used to load the expectation data after creation. Stores the data for later callback use.
     * @param data the data ({...}) to populate the expectation with
     */
    @Override
    protected void load(String data) {
        this.data = data;
    }

    /**
     * Method used in dumping the state of the expectation at the moment of evaluation.
     * Will provide the hint by supplier, if present.
     * @return string containing dump data.
     */
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
