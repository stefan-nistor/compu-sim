package ro.uaic.swqual.unit.util;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.util.Function3;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Function3Test {
    @Test
    void interfaceShouldAccept3ParamLambda() {
        Function3<Integer, String, Character, String> toStr = (num, str, chr) -> str + " " + num + " " + chr;
        assertEquals("prefix 1234 a", toStr.apply(1234, "prefix", 'a'));
    }
}
