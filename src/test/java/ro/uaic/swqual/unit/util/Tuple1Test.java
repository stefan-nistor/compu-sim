package ro.uaic.swqual.unit.util;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.util.Tuple1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class Tuple1Test {
    @Test
    void tupleOf1ShouldStoreCorrectly() {
        var t = new Tuple1<>("abc");
        assertInstanceOf(String.class, t.getFirst());
        assertEquals("abc", t.getFirst());
    }

    @Test
    void tupleOf1ShouldMapCorrectly() {
        var t = new Tuple1<>("abc");
        var l = t.map(String::length);
        assertEquals(3, l);
    }
}
