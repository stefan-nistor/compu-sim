package ro.uaic.swqual.model.operands;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.TestUtility;
import ro.uaic.swqual.exception.ParameterException;

public class LabelTest implements TestUtility {
    @Test
    public void labelShouldKeepLabel() {
        var label = new Label("test");
        Assert.assertEquals("test", label.getName());
    }

    @Test
    public void labelShouldThrowOnWrite() {
        var label = new Label("");
        Assert.assertThrows(ParameterException.class, () -> label.setValue((char) 0));
    }

    @Test
    public void labelShouldThrowOnReadChar() {
        var label = new Label("");
        Assert.assertThrows(ParameterException.class, () -> discard(label.getValue()));
    }
}
