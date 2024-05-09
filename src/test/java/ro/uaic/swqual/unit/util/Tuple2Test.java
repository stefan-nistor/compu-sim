package ro.uaic.swqual.unit.util;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.util.Tuple2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class Tuple2Test {
    @Test
    void tupleOf2ShouldStoreCorrectly() {
        var t = new Tuple2<>("abc", 5);
        assertInstanceOf(String.class, t.getFirst());
        assertInstanceOf(Integer.class, t.getSecond());
        assertEquals("abc", t.getFirst());
        assertEquals(5, t.getSecond());
    }

    @Test
    void tupleOf2ShouldMapCorrectly() {
        var t = new Tuple2<>("abc", 5);
        var l = t.map(String::length);
        assertEquals(3, l);
        var v = t.map((str, val) -> val);
        assertEquals(5, v);
        var concat = t.map((str, val) -> str + val);
        assertEquals("abc5", concat);
    }
}
