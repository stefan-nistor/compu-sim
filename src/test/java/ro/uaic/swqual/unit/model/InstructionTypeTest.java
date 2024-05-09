package ro.uaic.swqual.unit.model;

import org.junit.jupiter.api.Test;

import static ro.uaic.swqual.model.InstructionType.values;
import static ro.uaic.swqual.model.InstructionType.fromLabel;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionTypeTest {
    @Test
    void fromLabelShouldCoversAllCases() {
        assertTrue(stream(values()).allMatch(value -> fromLabel(value.label) == value));
    }
}
