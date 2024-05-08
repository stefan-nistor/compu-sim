package ro.uaic.swqual.unit.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.Label;

class LabelTest implements TestUtility {
    @Test
    void labelShouldKeepLabel() {
        var label = new Label("test");
        Assertions.assertEquals("test", label.getName());
    }

    @Test
    void labelShouldThrowOnWrite() {
        var label = new Label("");
        Assertions.assertThrows(ParameterException.class, () -> label.setValue((char) 0));
    }

    @Test
    void labelShouldThrowOnReadChar() {
        var label = new Label("");
        Assertions.assertThrows(ParameterException.class, () -> discard(label.getValue()));
    }
}
