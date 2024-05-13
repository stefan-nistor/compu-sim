package ro.uaic.swqual.unit.mem;

import ro.uaic.swqual.exception.ValueException;
import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.mem.ReadableWriteableMemoryUnit;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.RelativeMemoryLocation;
import ro.uaic.swqual.unit.TestUtility;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public interface MemTestUtility extends TestUtility {
    default MemoryUnit proxyRWMemoryUnit(
            Function<MemoryLocation, Character> mapper,
            BiConsumer<MemoryLocation, Character> consumer
    ) {
        return new ReadableWriteableMemoryUnit() {
            @Override
            public char read(MemoryLocation location) {
                return mapper.apply(location);
            }

            @Override
            public void write(MemoryLocation location, char value) {
                consumer.accept(location, value);
            }
        };
    }

    default AbsoluteMemoryLocation aloc(Parameter reg) {
        return new AbsoluteMemoryLocation(reg);
    }
    default ConstantMemoryLocation cloc(char value) { return new ConstantMemoryLocation(value); }

    default MemoryLocation rloc(List<Parameter> pList, List<BinaryOperator<Character>> rList) {
        assert pList.size() == rList.size() + 1;
        try {
            return new RelativeMemoryLocation(pList, rList);
        } catch (ValueException e) {
            return new ConstantMemoryLocation((char) 0x0);
        }
    }

    default MemoryLocation rloc(Parameter p0) {
        return rloc(List.of(p0), List.of());
    }

    default MemoryLocation rloc(
            Parameter p0,
            BinaryOperator<Character> r0,
            Parameter p1
    ) {
        return rloc(List.of(p0, p1), List.of(r0));
    }
}
