package ro.uaic.swqual.unit.util;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.util.Tuple3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class Tuple3Test {
    @Test
    void tupleOf3ShouldStoreCorrectly() {
        var t = new Tuple3<>("abc", 5, 'x');
        assertInstanceOf(String.class, t.getFirst());
        assertInstanceOf(Integer.class, t.getSecond());
        assertInstanceOf(Character.class, t.getThird());
        assertEquals("abc", t.getFirst());
        assertEquals(5, t.getSecond());
        assertEquals('x', t.getThird());
    }

    @Test
    void tupleOf3ShouldMapCorrectly() {
        var t = new Tuple3<>("abc", 5, 'x');
        var l = t.map(String::length);
        assertEquals(3, l);
        var v = t.map((str, val) -> val);
        assertEquals(5, v);
        var concat = t.map((str, val) -> str + val);
        assertEquals("abc5", concat);
        var c = t.map((str, val, chr) -> chr);
        assertEquals('x', c);
        concat = t.map((str, val, chr) -> str + val + chr);
        assertEquals("abc5x", concat);
    }
}
