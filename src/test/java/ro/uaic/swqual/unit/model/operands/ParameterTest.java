package ro.uaic.swqual.unit.model.operands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.RelativeMemoryLocation;
import ro.uaic.swqual.unit.proc.ProcTestUtility;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterTest implements RegisterTestUtility, ProcTestUtility {
    Parameter mockParameter(char givenValue) {
        return new Parameter() {{
            value = givenValue;
        }};
    }

    @Test
    void parameterShouldBeReadable() {
        var param = mockParameter((char) 10);
        Assertions.assertEquals(10, param.getValue());
    }

    @Test
    void parameterShouldNotBeWriteable() {
        var param = mockParameter((char) 40);
        Assertions.assertThrows(ParameterException.class, () -> param.setValue((char) 20));
    }

    @Test
    void equalsTest() {
        assertTrue(equalsCoverageTest(
                mockParameter((char) 0x1234),
                mockParameter((char) 0x1234),
                mockParameter((char) 0xDEAD),
                new Constant((char) 0x1234)
        ));
    }

    void parseValidIntValueShouldSucceed(String intVal, int actualVal) {
        assertEquals((char) actualVal, Parameter.parse(intVal).getValue());
    }

    @Test
    void parseValidBase2ConstantsShouldSucceed() {
        parseValidIntValueShouldSucceed("0b011", 0b011);
        parseValidIntValueShouldSucceed("0B10001", 0b10001);
        parseValidIntValueShouldSucceed("#0b011", 0b011);
        parseValidIntValueShouldSucceed("#0B10001", 0b10001);
    }

    @Test
    void parseValidBase8ConstantsShouldSucceed() {
        parseValidIntValueShouldSucceed("071", 57);
        parseValidIntValueShouldSucceed("052", 42);
        parseValidIntValueShouldSucceed("#071", 57);
        parseValidIntValueShouldSucceed("#052", 42);
    }

    @Test
    void parseValidBase10ConstantsShouldSucceed() {
        parseValidIntValueShouldSucceed("71", 71);
        parseValidIntValueShouldSucceed("52", 52);
        parseValidIntValueShouldSucceed("#71", 71);
        parseValidIntValueShouldSucceed("#52", 52);
    }
    @Test
    void parseValidBase16ConstantsShouldSucceed() {
        parseValidIntValueShouldSucceed("0x71af", 0x71af);
        parseValidIntValueShouldSucceed("0X52EF", 0x52ef);
        parseValidIntValueShouldSucceed("#0x71AF", 0x71af);
        parseValidIntValueShouldSucceed("#0X52ef", 0x52ef);
    }

    void parseInvalidIntValShouldThrow(String intVal) {
        assertThrows(ParameterException.class, () -> Parameter.parse(intVal));
    }

    @Test
    void parseInvalidBase2ConstantsShouldThrow() {
        parseInvalidIntValShouldThrow("0b");
        parseInvalidIntValShouldThrow("0B");
        parseInvalidIntValShouldThrow("#0b");
        parseInvalidIntValShouldThrow("#0B");
    }

    @Test
    void parseInvalidBase8ConstantsShouldThrow() {
        parseInvalidIntValShouldThrow("08");
        parseInvalidIntValShouldThrow("09");
        parseInvalidIntValShouldThrow("#08");
        parseInvalidIntValShouldThrow("#09");
    }

    @Test
    void parseInvalidBase10ConstantsShouldThrow() {
        parseInvalidIntValShouldThrow("1a");
        parseInvalidIntValShouldThrow("1b");
        parseInvalidIntValShouldThrow("#1a");
        parseInvalidIntValShouldThrow("#1b");
    }

    @Test
    void parseInvalidBase16ConstantsShouldThrow() {
        parseInvalidIntValShouldThrow("0xx");
        parseInvalidIntValShouldThrow("0XY");
        parseInvalidIntValShouldThrow("#0xX");
        parseInvalidIntValShouldThrow("#0Xy");
    }

    @Test
    void parseConstantMemLocShouldReturnValidConstMemLoc() {
        var p0 = Parameter.parse("[100]");
        assertInstanceOf(ConstantMemoryLocation.class, p0);
        assertEquals((char) 100, p0.getValue());
    }

    @Test
    void parseAbsoluteMemLocShouldReturnValidAbsMemLoc() {
        var p0 = Parameter.parse("[r0]");
        assertInstanceOf(AbsoluteMemoryLocation.class, p0);
        assertThrows(ParameterException.class, () -> discard(p0.getValue()));
        var l0 = (MemoryLocation)p0;
        var r0 = reg(0x50);
        l0.resolveInnerReferences(Map.of("r0", r0));
        assertEquals(0x50, p0.getValue());
    }

    @Test
    void parseRelativeMemLocShouldReturnRelMemLoc() {
        var p0 = Parameter.parse("[r1 + 50 - r3]");
        assertInstanceOf(RelativeMemoryLocation.class, p0);
        assertThrows(ParameterException.class, () -> discard(p0.getValue()));
        var l0 = (MemoryLocation)p0;
        var r1 = reg();
        l0.resolveInnerReferences(Map.of("r1", r1));
        assertThrows(ParameterException.class, () -> discard(p0.getValue()));
        var r3 = reg();
        l0.resolveInnerReferences(Map.of("r3", r3));
        assertEquals(50, p0.getValue());
        r3.setValue((char) 25);
        assertEquals((char) 25, p0.getValue());
        r1.setValue((char) 100);
        assertEquals((char) 125, p0.getValue());
    }
}
