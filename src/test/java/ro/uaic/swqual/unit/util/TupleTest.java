package ro.uaic.swqual.unit.util;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple1;
import ro.uaic.swqual.util.Tuple2;
import ro.uaic.swqual.util.Tuple3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TupleTest {
    @Test
    void tupleOf1IsFormed() {
        var t = Tuple.of(5);
        assertInstanceOf(Tuple1.class, t);
        assertInstanceOf(Integer.class, t.getFirst());
        assertEquals(5, t.getFirst());
    }

    @Test
    void tupleOf2IsFormed() {
        var t = Tuple.of(5, "abc");
        assertInstanceOf(Tuple2.class, t);
        assertInstanceOf(Integer.class, t.getFirst());
        assertInstanceOf(String.class, t.getSecond());
        assertEquals(5, t.getFirst());
        assertEquals("abc", t.getSecond());
    }

    @Test
    void tupleOf3IsFormed() {
        var t = Tuple.of(5, "abc", 'x');
        assertInstanceOf(Tuple3.class, t);
        assertInstanceOf(Integer.class, t.getFirst());
        assertInstanceOf(String.class, t.getSecond());
        assertInstanceOf(Character.class, t.getThird());
        assertEquals(5, t.getFirst());
        assertEquals("abc", t.getSecond());
        assertEquals('x', t.getThird());
    }
}
