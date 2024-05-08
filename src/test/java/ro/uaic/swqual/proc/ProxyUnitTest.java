package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.mem.MemTestUtility;
import ro.uaic.swqual.mem.MemoryUnit;
import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.mem.ReadableWriteableMemoryUnit;
import ro.uaic.swqual.mem.WriteableMemoryUnit;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.ResolvedMemory;
import ro.uaic.swqual.model.operands.UnresolvedMemory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ProxyUnitTest implements ProcTestUtility, MemTestUtility {
    public ProxyUnit<MemoryUnit> mockProxyUnit(FlagRegister register) {
        return new ProxyUnit<>() {
            @Override
            public void raiseFlag(char value) {
                register.set(value);
            }
        };
    }

    public MemoryUnit proxyROMemoryUnit(
            Function <MemoryLocation, Character> mapper
    ) {
        return (ReadableMemoryUnit) mapper::apply;
    }

    public MemoryUnit proxyWOMemoryUnit(
            BiConsumer<MemoryLocation, Character> consumer
    ) {
        return (WriteableMemoryUnit) consumer::accept;
    }

    public MemoryUnit proxyRWMemoryUnit(
            Function <MemoryLocation, Character> mapper,
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

    @Test
    public void locateOfNonMemoryLocationShouldActAsIdentity() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var c0 = _const(10);
        var r0 = reg();
        Assert.assertEquals(c0, unit.locate(c0));
        Assert.assertEquals(r0, unit.locate(r0));
    }

    @Test
    public void locateOfNonHWShouldReturnUnresolvedMemoryAndNotRaiseSegFlag() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var loc = dloc((char) 0x100);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    public void locateOfNonHWShouldReturnUnresolvedMemoryThatRaisesSegOnAccess() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var loc = dloc((char) 0x100);
        var mem = unit.locate(loc);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        discard(mem.getValue());
        Assert.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    public void locateOfMultipleHWShouldReturnUnresolvedMemoryAndRaiseMultistateFlag() {
        var freg = freg();
        var unit = mockProxyUnit(freg);

        var hw0 = proxyRWMemoryUnit(null, null);
        var hw1 = proxyRWMemoryUnit(null, null);
        // Overlap [0x80, 0x100)
        unit.registerHardwareUnit(hw0, (char) 0x50, (char) 0x100);
        unit.registerHardwareUnit(hw1, (char) 0x80, (char) 0x200);

        var loc = dloc((char) 0x90);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        Assert.assertTrue(freg.isSet(FlagRegister.MULTISTATE_FLAG));
    }

    @Test
    public void locateOfHWAndDelegateOverlapShouldReturnUnresolvedMemoryAndRaiseMultistateFlag() {
        var freg = freg();
        var unit = mockProxyUnit(freg);

        var hw0 = proxyRWMemoryUnit(null, null);
        var locator = singleLocationUnit(freg);
        // Overlap [0x80, 0x100)
        unit.registerHardwareUnit(hw0, (char) 0x50, (char) 0x100);
        unit.registerLocator(locator, (char) 0x80, (char) 0x200);

        var loc = dloc((char) 0x90);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        Assert.assertTrue(freg.isSet(FlagRegister.MULTISTATE_FLAG));
    }

    @Test
    public void locateOfMultipleHWShouldReturnUnresolvedMemoryThatRaisesSegOnAccess() {
        var freg = freg();
        var unit = mockProxyUnit(freg);

        var hw0 = proxyRWMemoryUnit(null, null);
        var hw1 = proxyRWMemoryUnit(null, null);
        // Overlap [0x80, 0x100)
        unit.registerHardwareUnit(hw0, (char) 0x50, (char) 0x100);
        unit.registerHardwareUnit(hw1, (char) 0x80, (char) 0x200);

        var loc = dloc((char) 0x90);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        discard(mem.getValue());
        Assert.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    public void locateOfSingleHWShouldResolveCorrectly() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var hw = proxyRWMemoryUnit(null, null);

        unit.registerHardwareUnit(hw, (char) 0x50, (char) 0x100);
        var loc = dloc((char) 0x75);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);
    }

    @Test
    public void locateOfSingleRWHWShouldReturnResolvedMemoryThatIsReadWriteable() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var storage = new AtomicInteger(0);
        var hw = proxyRWMemoryUnit(
                (loc) -> (char) storage.get(),
                (loc, value) -> storage.set(value)
        );

        unit.registerHardwareUnit(hw, (char) 0x50, (char) 0x100);
        var loc = dloc((char) 0x75);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);
        discard(mem.getValue());
        Assert.assertEquals(0, freg.getValue());

        mem.setValue((char) 10);
        Assert.assertEquals(10, mem.getValue());
    }

    @Test
    public void locateOfSingleROHWShouldReturnResolvedMemoryThatIsReadableAndRaisesSegOnWrite() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var storage = new AtomicInteger(0);
        var hw = proxyROMemoryUnit(
                loc -> (char) storage.get()
        );

        unit.registerHardwareUnit(hw, (char) 0x50, (char) 0x100);
        var loc = dloc((char) 0x75);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);
        discard(mem.getValue());
        Assert.assertEquals(0, freg.getValue());

        storage.set(20);
        Assert.assertEquals(20, mem.getValue());
        Assert.assertEquals(0, freg.getValue());

        mem.setValue((char) 10);
        Assert.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    public void locateOfSingleWOHWShouldReturnResolvedMemoryThatIsWriteableAndRaisesSegOnRead() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var storage = new AtomicInteger(0);
        var hw = proxyWOMemoryUnit(
                (loc, val) -> storage.set(val)
        );

        unit.registerHardwareUnit(hw, (char) 0x50, (char) 0x100);
        var loc = dloc((char) 0x75);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);
        discard(mem.getValue());
        Assert.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));

        freg.clear();
        mem.setValue((char) 20);
        Assert.assertEquals(0, freg.getValue());
        Assert.assertEquals(20, storage.get());
    }

    @Test
    public void locateOfSingleHWShouldReturnUnresolvedMemoryWhenOutOfRange() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var hw0 = proxyRWMemoryUnit(null, null);

        unit.registerHardwareUnit(hw0, (char) 0x50, (char) 0xB0);
        var loc = dloc((char) 0x49);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);

        loc = dloc((char) 0x50);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFE);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFF);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
    }

    @Test
    public void locateOfSingleMappedByPredicateLocationShouldReturnUnresolvedMemoryWhenOutOfRange() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var hw = proxyRWMemoryUnit(null, null);

        unit.registerHardwareUnit(hw, (char) 0x50, addr -> addr >= 0x50 && addr + 1 < 0x100);
        var loc = dloc((char) 0x49);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);

        loc = dloc((char) 0x50);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFE);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFF);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
    }

    @Test
    public void locateOfAdjacentHwAndLocatorShouldReturnResolveInRangeAndNotResolveOutOfRange() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var hw = proxyRWMemoryUnit(null, null);
        var locator = singleLocationUnit(freg);

        unit.registerHardwareUnit(hw, (char) 0x50, (char) 0xB0);
        unit.registerLocator(locator, (char)0x100, (char) 0x100);
        var loc = dloc((char) 0x49);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);

        loc = dloc((char) 0x50);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFE);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFF); // 1 byte in hw, 1 byte in loc
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);

        loc = dloc((char) 0x100);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0x1FE);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0x1FF);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
    }

    @Test
    public void locateOfAdjacentLocatorAndHwShouldReturnResolveInRangeAndNotResolveOutOfRange() {
        var freg = freg();
        var unit = mockProxyUnit(freg);
        var hw = proxyRWMemoryUnit(null, null);
        var locator = singleLocationUnit(freg);

        unit.registerLocator(locator, (char) 0x50, (char) 0xB0);
        unit.registerHardwareUnit(hw, (char)0x100, (char) 0x100);
        var loc = dloc((char) 0x49);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);

        loc = dloc((char) 0x50);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFE);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0xFF); // 1 byte in hw, 1 byte in loc
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);

        loc = dloc((char) 0x100);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0x1FE);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);

        loc = dloc((char) 0x1FF);
        mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
    }
}
