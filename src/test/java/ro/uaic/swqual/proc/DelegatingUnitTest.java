package ro.uaic.swqual.proc;

import org.junit.Assert;
import org.junit.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.mem.MemTestUtility;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.ResolvedMemory;
import ro.uaic.swqual.model.operands.UnresolvedMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DelegatingUnitTest implements ProcTestUtility, MemTestUtility {
    DelegatingUnit mockDelegatingUnit(FlagRegister register) {
        return new DelegatingUnit() {
            @Override
            public void raiseFlag(char value) {
                register.set(value);
            }
        };
    }

    @Test
    public void locateOfNonMemoryLocationShouldActAsIdentity() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var c0 = _const(10);
        var r0 = reg();
        Assert.assertEquals(c0, unit.locate(c0));
        Assert.assertEquals(r0, unit.locate(r0));
    }

    @Test
    public void locateOfNonMappedMemoryLocationShouldReturnUnresolvedMemoryAndNotRaiseSegFlag() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var loc = dloc((char) 0x100);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    public void locateOfNonMappedMemoryLocationShouldReturnUnresolvedMemoryThatRaisesSegOnAccess() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var loc = dloc((char) 0x100);
        var mem = unit.locate(loc);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        discard(mem.getValue());
        Assert.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    public void locateOfMultipleMappedMemoryLocationShouldReturnUnresolvedMemoryAndRaiseMultistateFlag() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        var subLoc0 = mockDelegatingUnit(freg);
        var subLoc1 = mockDelegatingUnit(freg);
        // Overlap [0x80, 0x100)
        unit.registerLocator(subLoc0, (char) 0x50, (char) 0x100);
        unit.registerLocator(subLoc1, (char) 0x80, (char) 0x200);

        var loc = dloc((char) 0x90);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        Assert.assertTrue(freg.isSet(FlagRegister.MULTISTATE_FLAG));
    }

    @Test
    public void locateOfMultipleMappedMemoryLocationShouldReturnUnresolvedMemoryThatRaisesSegOnAccess() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        var subLoc0 = mockDelegatingUnit(freg);
        var subLoc1 = mockDelegatingUnit(freg);
        // Overlap [0x80, 0x100)
        unit.registerLocator(subLoc0, (char) 0x50, (char) 0x100);
        unit.registerLocator(subLoc1, (char) 0x80, (char) 0x200);

        var loc = dloc((char) 0x90);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof UnresolvedMemory);
        Assert.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        discard(mem.getValue());
        Assert.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    public void locateOfSingleMappedMemoryLocationShouldReturnResolvedMemory() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0x100);
        var loc = dloc((char) 0x75);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);
    }

    @Test
    public void locateOfSingleMappedMemoryLocationShouldReturnResolvedMemoryAndNotRaiseAnyFlag() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0x100);
        var loc = dloc((char) 0x75);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);
        discard(mem.getValue());
        Assert.assertEquals(0, freg.getValue());
    }

    @Test
    public void locateOfSingleMappedMemoryLocationShouldReturnResolvedMemoryThatStores() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0x100);
        var loc = dloc((char) 0x75);
        var mem = unit.locate(loc);
        Assert.assertTrue(mem instanceof ResolvedMemory);
        mem.setValue((char) 0x256);

        var anotherMem = unit.locate(dloc((char) 0x75));
        Assert.assertEquals(0x256, anotherMem.getValue());
    }

    @Test
    public void locateOfSingleMappedMemoryLocationShouldReturnUnresolvedMemoryWhenOutOfRange() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0xB0);
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
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, addr -> addr >= 0x50 && addr + 1 < 0x100);
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

    ProcessingUnit proxyExecutor(FlagRegister freg, Consumer<Instruction> onExecute) {
        return new ProcessingUnit() {
            @Override
            public void execute(Instruction instruction) throws InstructionException, ParameterException {
                onExecute.accept(instruction);
            }

            @Override
            public void raiseFlag(char value) {
                freg.set(value);
            }
        };
    }

    @Test
    public void registerExecutorShouldStoreForLaterExecution() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        AtomicReference<List<Instruction>> proxy0ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec0 = proxyExecutor(freg, instruction -> proxy0ReceivedInstructions.get().add(instruction));

        AtomicReference<List<Instruction>> proxy1ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec1 = proxyExecutor(freg, instruction -> proxy1ReceivedInstructions.get().add(instruction));

        unit.registerExecutor(proxyExec0);
        unit.registerExecutor(proxyExec1);

        unit.execute(new Instruction());
        unit.execute(new Instruction());

        Assert.assertEquals(2, proxy0ReceivedInstructions.get().size());
        Assert.assertEquals(2, proxy1ReceivedInstructions.get().size());
    }

    @Test
    public void registerFilteredExecutorShouldFilter() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        AtomicReference<List<Instruction>> proxy0ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec0 = proxyExecutor(freg, instruction -> proxy0ReceivedInstructions.get().add(instruction));

        AtomicReference<List<Instruction>> proxy1ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec1 = proxyExecutor(freg, instruction -> proxy1ReceivedInstructions.get().add(instruction));

        unit.registerExecutor(proxyExec0, instruction -> instruction.getType() == InstructionType.ALU_ADD);
        unit.registerExecutor(proxyExec1, instruction -> instruction.getType() == InstructionType.ALU_SUB);

        unit.execute(new Instruction(InstructionType.ALU_ADD));
        unit.execute(new Instruction(InstructionType.ALU_SUB));
        unit.execute(new Instruction(InstructionType.ALU_UMUL));

        Assert.assertEquals(1, proxy0ReceivedInstructions.get().size());
        Assert.assertEquals(1, proxy1ReceivedInstructions.get().size());
        Assert.assertEquals(InstructionType.ALU_ADD, proxy0ReceivedInstructions.get().getFirst().getType());
        Assert.assertEquals(InstructionType.ALU_SUB, proxy1ReceivedInstructions.get().getFirst().getType());
    }

    @Test
    public void registerFilterExecutorShouldProvideAValidDefaultFilter() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        AtomicReference<List<Instruction>> proxy0ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec0 = proxyExecutor(freg, instruction -> proxy0ReceivedInstructions.get().add(instruction));

        AtomicReference<List<Instruction>> proxy1ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec1 = proxyExecutor(freg, instruction -> proxy1ReceivedInstructions.get().add(instruction));

        unit.registerExecutor(proxyExec0, instruction -> instruction.getType() == InstructionType.ALU_ADD);
        unit.registerExecutor(proxyExec1, instruction -> instruction.getType() == InstructionType.ALU_SUB);

        var filter = unit.getDefaultFilter();
        Assert.assertTrue(
                Stream.of(
                        InstructionType.ALU_ADD,
                        InstructionType.ALU_SUB,
                        InstructionType.ALU_UMUL
                )
                        .map(Instruction::new)
                        .filter(filter)
                        .map(Instruction::getType)
                        .allMatch(type -> type == InstructionType.ALU_ADD || type == InstructionType.ALU_SUB)
        );
    }
}
