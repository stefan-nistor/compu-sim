package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.Label;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelTest implements TestUtility, RegisterTestUtility, ProcTestUtility {
    @Test
    void labelShouldKeepLabel() {
        var label = new Label("test");
        assertEquals("test", label.getName());
    }

    @Test
    void labelShouldThrowOnWrite() {
        var label = new Label("");
        assertThrows(ParameterException.class, () -> label.setValue((char) 0));
    }

    @Test
    void labelShouldThrowOnReadChar() {
        var label = new Label("");
        assertThrows(ParameterException.class, () -> discard(label.getValue()));
    }

    @Test
    void labelEqualsTest() {
        assertTrue(equalsCoverageTest(
                new Label("test"),
                new Label("test"),
                new Label("diff"),
                new Constant((char) 0xABCD)
        ));
    }
}
