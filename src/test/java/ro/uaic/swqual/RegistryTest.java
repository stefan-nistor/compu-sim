package ro.uaic.swqual;

import org.junit.Assert;
import org.junit.Test;

public class RegistryTest {
    @Test
    public void getNameTest() {
        var reg = new Registry();
        reg.setName("test");
        Assert.assertEquals(reg.getName(), "test");
    }
}
