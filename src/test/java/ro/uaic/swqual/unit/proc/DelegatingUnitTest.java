package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.unit.mem.MemTestUtility;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.model.operands.ResolvedMemory;
import ro.uaic.swqual.model.operands.UnresolvedMemory;
import ro.uaic.swqual.proc.DelegatingUnit;
import ro.uaic.swqual.proc.ProcessingUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

class DelegatingUnitTest implements ProcTestUtility, MemTestUtility {
    DelegatingUnit mockDelegatingUnit(FlagRegister register) {
        return new DelegatingUnit() {
            @Override
            public void raiseFlag(char value) {
                register.set(value);
            }
        };
    }

    @Test
    void locateOfNonMemoryLocationShouldActAsIdentity() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var c0 = _const(10);
        var r0 = reg();
        Assertions.assertEquals(c0, unit.locate(c0));
        Assertions.assertEquals(r0, unit.locate(r0));
    }

    @Test
    void locateOfNonMappedMemoryLocationShouldReturnUnresolvedMemoryAndNotRaiseSegFlag() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var loc = cloc((char) 0x100);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(UnresolvedMemory.class, mem);
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    void locateOfNonMappedMemoryLocationShouldReturnUnresolvedMemoryThatRaisesSegOnAccess() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var loc = cloc((char) 0x100);
        var mem = unit.locate(loc);
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        discard(mem.getValue());
        Assertions.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    void locateOfMultipleMappedMemoryLocationShouldReturnUnresolvedMemoryAndRaiseMultistateFlag() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        var subLoc0 = mockDelegatingUnit(freg);
        var subLoc1 = mockDelegatingUnit(freg);
        // Overlap [0x80, 0x100)
        unit.registerLocator(subLoc0, (char) 0x50, (char) 0x100);
        unit.registerLocator(subLoc1, (char) 0x80, (char) 0x200);

        var loc = cloc((char) 0x90);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(UnresolvedMemory.class, mem);
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        Assertions.assertTrue(freg.isSet(FlagRegister.MULTISTATE_FLAG));
    }

    @Test
    void locateOfMultipleMappedMemoryLocationShouldReturnUnresolvedMemoryThatRaisesSegOnAccess() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        var subLoc0 = mockDelegatingUnit(freg);
        var subLoc1 = mockDelegatingUnit(freg);
        // Overlap [0x80, 0x100)
        unit.registerLocator(subLoc0, (char) 0x50, (char) 0x100);
        unit.registerLocator(subLoc1, (char) 0x80, (char) 0x200);

        var loc = cloc((char) 0x90);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(UnresolvedMemory.class, mem);
        Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        discard(mem.getValue());
        Assertions.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));
    }

    @Test
    void locateOfSingleMappedMemoryLocationShouldReturnResolvedMemory() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0x100);
        var loc = cloc((char) 0x75);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(ResolvedMemory.class, mem);
    }

    @Test
    void locateOfSingleMappedMemoryLocationShouldReturnResolvedMemoryAndNotRaiseAnyFlag() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0x100);
        var loc = cloc((char) 0x75);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(ResolvedMemory.class, mem);
        discard(mem.getValue());
        Assertions.assertEquals(0, freg.getValue());
    }

    @Test
    void locateOfSingleMappedMemoryLocationShouldReturnResolvedMemoryThatStores() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0x100);
        var loc = cloc((char) 0x75);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(ResolvedMemory.class, mem);
        mem.setValue((char) 0x256);

        var anotherMem = unit.locate(cloc((char) 0x75));
        Assertions.assertEquals(0x256, anotherMem.getValue());
    }

    @Test
    void locateOfSingleMappedMemoryLocationShouldReturnUnresolvedMemoryWhenOutOfRange() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, (char) 0xB0);
        var loc = cloc((char) 0x49);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(UnresolvedMemory.class, mem);

        loc = cloc((char) 0x50);
        mem = unit.locate(loc);
        Assertions.assertInstanceOf(ResolvedMemory.class, mem);

        loc = cloc((char) 0xFE);
        mem = unit.locate(loc);
        Assertions.assertInstanceOf(ResolvedMemory.class, mem);

        loc = cloc((char) 0xFF);
        mem = unit.locate(loc);
        Assertions.assertInstanceOf(UnresolvedMemory.class, mem);
    }

    @Test
    void locateOfSingleMappedByPredicateLocationShouldReturnUnresolvedMemoryWhenOutOfRange() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);
        var resolvingSubUnit = singleLocationUnit(freg);

        unit.registerLocator(resolvingSubUnit, (char) 0x50, addr -> addr >= 0x50 && addr + 1 < 0x100);
        var loc = cloc((char) 0x49);
        var mem = unit.locate(loc);
        Assertions.assertInstanceOf(UnresolvedMemory.class, mem);

        loc = cloc((char) 0x50);
        mem = unit.locate(loc);
        Assertions.assertInstanceOf(ResolvedMemory.class, mem);

        loc = cloc((char) 0xFE);
        mem = unit.locate(loc);
        Assertions.assertInstanceOf(ResolvedMemory.class, mem);

        loc = cloc((char) 0xFF);
        mem = unit.locate(loc);
        Assertions.assertInstanceOf(UnresolvedMemory.class, mem);
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
    void registerExecutorShouldStoreForLaterExecution() {
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

        Assertions.assertEquals(2, proxy0ReceivedInstructions.get().size());
        Assertions.assertEquals(2, proxy1ReceivedInstructions.get().size());
    }

    @Test
    void registerFilteredExecutorShouldFilter() {
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

        Assertions.assertEquals(1, proxy0ReceivedInstructions.get().size());
        Assertions.assertEquals(1, proxy1ReceivedInstructions.get().size());
        Assertions.assertEquals(InstructionType.ALU_ADD, proxy0ReceivedInstructions.get().getFirst().getType());
        Assertions.assertEquals(InstructionType.ALU_SUB, proxy1ReceivedInstructions.get().getFirst().getType());
    }

    @Test
    void registerFilterExecutorShouldProvideAValidDefaultFilter() {
        var freg = freg();
        var unit = mockDelegatingUnit(freg);

        AtomicReference<List<Instruction>> proxy0ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec0 = proxyExecutor(freg, instruction -> proxy0ReceivedInstructions.get().add(instruction));

        AtomicReference<List<Instruction>> proxy1ReceivedInstructions = new AtomicReference<>(new ArrayList<>());
        var proxyExec1 = proxyExecutor(freg, instruction -> proxy1ReceivedInstructions.get().add(instruction));

        unit.registerExecutor(proxyExec0, instruction -> instruction.getType() == InstructionType.ALU_ADD);
        unit.registerExecutor(proxyExec1, instruction -> instruction.getType() == InstructionType.ALU_SUB);

        var filter = unit.getDefaultFilter();
        Assertions.assertTrue(
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
