package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.exception.InstructionException;
import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.mem.RandomAccessMemory;
import ro.uaic.swqual.proc.ProcessingUnit;
import ro.uaic.swqual.unit.mem.MemTestUtility;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.proc.MemoryManagementUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryManagementUnitTest implements ProcTestUtility, MemTestUtility {
    @Test
    void movRegRegTest() {
        var r0 = reg(10);
        var r1 = reg();
        var sp = reg();
        var mmu = new MemoryManagementUnit(freg(), sp);
        mmu.execute(new Instruction(InstructionType.MMU_MOV, r0, r1));
        assertEquals(r0.getValue(), r1.getValue());
    }

    @Test
    void movRegRamTest() {
        exceptionLess(() -> {
            var sp = reg();
            var freg = freg();

            var mmu = new MemoryManagementUnit(freg, sp);
            var ram = new RandomAccessMemory((char) 0x1000, freg);

            mmu.registerHardwareUnit(ram, (char) 0x0000, (location) -> location >= 0x100 && location <= 0x1000);

            var addr = reg(0x50); // mov r0 0x50
            var loc = new AbsoluteMemoryLocation(addr); // define [r0] (add pointer)

            var location = mmu.locate(loc); // locate [r0]
            Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
            discard(location.getValue()); // actually access [r0]
            assertTrue(freg.isSet(FlagRegister.SEG_FLAG));


            addr.setValue(0x150);
            location = mmu.locate(loc);
            freg.clear();

            Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
            discard(location.getValue());
            Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        });
    }

    @Test
    void delegateRamShouldBeDifferentFromMmuRam() {
        exceptionLess(() -> {
            var freg = freg();
            var sp = reg();
            var mmu0 = new MemoryManagementUnit(freg, sp);
            var mmu1 = new MemoryManagementUnit(freg, sp);
            var ram0 = new RandomAccessMemory(0x1000, freg);
            var ram1 = new RandomAccessMemory(0x2000, freg);

            // Offsets and comparators are RELATIVE to their unit
            // MMU0 has:
            //   - RAM0 at offset 0x0000 ranging [0x0000, 0x1000)
            //   - MMU1 at offset 0x2000 ranging [0x2000, 0x4000)
            // MMU1 has:
            //   - RAM1 ranging [0x0000, 0x2000)
            // => MMU1 HW will range:
            //      absoluteRangeOf(RAM1) = offsetOf(RAM1) + rangeOf(RAM1)
            //                            = offsetOf(MMU0.MMU1) + offsetOf(MMU1.RAM1) + range(RAM1)
            //                            = 0x2000 + 0x0000 + [0x0000, 0x2000)
            //                            = [0x2000, 0x4000)

            // Relative to MM0
            mmu0.registerHardwareUnit(ram0, (char) 0x0000, (location) -> location + 1 < 0x1000);
            mmu0.registerLocator(mmu1, (char) 0x2000, location -> location >= 0x2000 && location + 1 < 0x4000);

            // Relative to MM1
            mmu1.registerHardwareUnit(ram1, (char) 0x0000, (location) -> location + 1 < 0x2000);

            Function<Boolean, BiPredicate<Character, Character>> memoryValidator = (validity) ->
                    (start, end) -> IntStream.range(start, end)
                            .mapToObj(addrVal -> {
                                freg.clear();
                                discard(mmu0.locate(aloc(reg(addrVal))).getValue());
                                return null;
                            })
                            .allMatch(ignored -> freg.isSet(FlagRegister.SEG_FLAG) != validity);
            assertTrue(memoryValidator.apply(true).test((char) 0, (char) 0x0FFF));
            assertTrue(memoryValidator.apply(false).test((char) 0x0FFF, (char) 0x2000));
            assertTrue(memoryValidator.apply(true).test((char) 0x2000, (char) 0x3FFF));
            assertTrue(memoryValidator.apply(false).test((char) 0x3FFF, (char) 0x6000));
        });
    }

    @Test
    void rangeBasedDelegateRamShouldBeDifferentFromMmuRam() {
        exceptionLess(() -> {
            var freg = freg();
            var sp = reg();

            // Offsets and comparators are RELATIVE to their unit
            // MMU0 has:
            //   - RAM0 at offset 0x0000 ranging [0x0000, 0x1000)
            //   - MMU1 at offset 0x2000 ranging [0x2000, 0x4000)
            // MMU1 has:
            //   - RAM1 ranging [0x0000, 0x2000)
            // => MMU1 HW will range:
            //      absoluteRangeOf(RAM1) = offsetOf(RAM1) + rangeOf(RAM1)
            //                            = offsetOf(MMU0.MMU1) + offsetOf(MMU1.RAM1) + range(RAM1)
            //                            = 0x2000 + 0x0000 + [0x0000, 0x2000)
            //                            = [0x2000, 0x4000)

            var mmu0 = new MemoryManagementUnit(freg, sp);
            var mmu1 = new MemoryManagementUnit(freg, sp);

            var ram0Size = (char) 0x1000;
            var ram1Size = (char) 0x2000;

            var ram0 = new RandomAccessMemory(ram0Size, freg);
            var ram1 = new RandomAccessMemory(ram1Size, freg);

            // Relative to MM0
            mmu0.registerHardwareUnit(ram0, (char) 0x0000, ram0Size);
            mmu0.registerLocator(mmu1, (char) 0x2000, ram1Size);

            // Relative to MM1
            mmu1.registerHardwareUnit(ram1, (char) 0x0000, ram1Size);

            Function<Boolean, BiPredicate<Character, Character>> memoryValidator = (validity) ->
                    (start, end) -> IntStream.range(start, end)
                            .mapToObj(addrVal -> {
                                freg.clear();
                                discard(mmu0.locate(aloc(reg(addrVal))).getValue());
                                return null;
                            })
                            .allMatch(ignored -> freg.isSet(FlagRegister.SEG_FLAG) != validity);
            assertTrue(memoryValidator.apply(true).test((char) 0, (char) 0x0FFF));
            assertTrue(memoryValidator.apply(false).test((char) 0x0FFF, (char) 0x2000));
            assertTrue(memoryValidator.apply(true).test((char) 0x2000, (char) 0x3FFF));
            assertTrue(memoryValidator.apply(false).test((char) 0x3FFF, (char) 0x6000));
        });
    }

    @Test
    void movWithRamShouldSuccessfullyStoreAndRead() {
        var freg = freg();
        var mmu0 = new MemoryManagementUnit(freg, reg());
        var storage = new AtomicInteger(0);
        var ramProxy = proxyRWMemoryUnit(l -> (char) storage.get(), (l, v) -> storage.set(v));
        mmu0.registerHardwareUnit(ramProxy, (char) 0, (char) 0x100);

        var r0 = reg((char) 0xAB);
        var loc = dloc((char) 0x80); // in "range"

        mmu0.execute(mov(loc, r0));
        assertEquals(0xAB, storage.get());

        storage.set(0xFF);
        mmu0.execute(mov(r0, loc));
        assertEquals(0xFF, r0.getValue());
    }

    @Test
    void movBetweenUnitsShouldStoreSuccessfully() {
        var freg = freg();
        var mmu0 = new MemoryManagementUnit(freg, reg());
        var storage0 = new AtomicInteger(0);
        var storage1 = new AtomicInteger(0);
        var ramProxy0 = proxyRWMemoryUnit(l -> (char) storage0.get(), (l, v) -> storage0.set(v));
        var ramProxy1 = proxyRWMemoryUnit(l -> (char) storage1.get(), (l, v) -> storage1.set(v));
        mmu0.registerHardwareUnit(ramProxy0, (char) 0, (char) 0x100);
        mmu0.registerHardwareUnit(ramProxy1, (char) 0x100, (char) 0x100);

        var r0 = reg((char) 0xAB);
        var loc0 = dloc((char) 0x80); // in "range" of ramProxy0
        var loc1 = dloc((char) 0x180); // in "range" of ramProxy1

        mmu0.execute(mov(loc0, r0));
        assertEquals(0xAB, storage0.get());

        mmu0.execute(mov(loc1, loc0));
        assertEquals(0xAB, storage1.get());

        var r1 = reg();
        mmu0.execute(mov(r1, loc1));
        assertEquals(0xAB, r1.getValue());
    }

    @Test
    void defaultFilterShouldAcceptOnlyMmuOperations() {
        var mmu = new MemoryManagementUnit(freg(), reg());
        var pred = mmu.getDefaultFilter();
        assertEquals(
                InstructionType.MMU_MOV,
                Stream.of(
                        InstructionType.ALU_ADD,
                        InstructionType.IPU_JMP,
                        InstructionType.MMU_MOV,
                        InstructionType.LABEL
                )
                        .filter(i -> pred.test(new Instruction(i)))
                        .sorted() // force eval of all filters
                        .findAny().orElse(InstructionType.LABEL)
        );
    }

    @Test
    void executeInvalidInstructionShouldThrow() {
        var mmu = new MemoryManagementUnit(freg(), reg());
        assertThrows(InstructionException.class, () -> mmu.execute(add(null, null)));
    }

    @Test
    void executePushShouldRequestMoveOfStackPointer() {
        exceptionLess(() -> {
            var sp = reg();
            var freg = freg();
            var mmu = new MemoryManagementUnit(freg, sp);
            var ram = new RandomAccessMemory(0x1000, freg);
            mmu.registerHardwareUnit(ram, (char) 0x0000, (char) 0x1000);

            var executor = new ProcessingUnit() {
                final List<Instruction> receivedInstructions = new ArrayList<>();
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    receivedInstructions.add(instruction);
                }

                @Override
                public void raiseFlag(char value) {
                    freg.set(value);
                }
            };
            mmu.registerExecutor(executor);

            var r0 = reg((char) 10);
            var r1 = reg((char) 20);
            var c0 = _const((char) 30);
            var spOffsetOnPush = _const((char) 2);
            var loc = aloc(sp);

            mmu.execute(push(r0));
            mmu.execute(push(r1));
            mmu.execute(push(c0));
            assertEquals(
                    List.of(add(sp, spOffsetOnPush), add(sp, spOffsetOnPush), add(sp, spOffsetOnPush)),
                    executor.receivedInstructions
            );

            var top = reg();
            mmu.execute(mov(top, loc));
            // Should be 30, as nobody executed the requested instructions, and so the last value pushed is on the sp, which is 0.
            assertEquals(0, sp.getValue());
            assertEquals(30, top.getValue());
        });
    }

    @Test
    void executePopShouldRequestMoveOfStackPointer() {
        exceptionLess(() -> {
            var sp = reg((char) 6); // setting to 6, since a 0 value and request of pop will trigger SEG_FLAG
            var freg = freg();
            var mmu = new MemoryManagementUnit(freg, sp);
            var ram = new RandomAccessMemory(0x1000, freg);
            mmu.registerHardwareUnit(ram, (char) 0x0000, (char) 0x1000);

            var executor = new ProcessingUnit() {
                final List<Instruction> receivedInstructions = new ArrayList<>();
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    receivedInstructions.add(instruction);
                }

                @Override
                public void raiseFlag(char value) {
                    freg.set(value);
                }
            };
            mmu.registerExecutor(executor);

            var r0 = reg((char) 10);
            var spOffsetOnPush = _const((char) 2);

            mmu.execute(pop(r0));
            mmu.execute(pop(r0));
            mmu.execute(pop(r0));
            assertEquals(
                    List.of(sub(sp, spOffsetOnPush), sub(sp, spOffsetOnPush), sub(sp, spOffsetOnPush)),
                    executor.receivedInstructions
            );

            // Should be 0, as no real executor is attached
            assertEquals(6, sp.getValue());
        });
    }

    @Test
    void executePopOnEmptyStackShouldRaiseSegFlag() {
        exceptionLess(() -> {
            var sp = reg((char) 0); // empty stack
            var freg = freg();
            var mmu = new MemoryManagementUnit(freg, sp);
            var ram = new RandomAccessMemory(0x1000, freg);
            mmu.registerHardwareUnit(ram, (char) 0x0000, (char) 0x1000);

            var executor = new ProcessingUnit() {
                final List<Instruction> receivedInstructions = new ArrayList<>();
                public void execute(Instruction instruction) throws InstructionException, ParameterException {
                    receivedInstructions.add(instruction);
                }

                @Override
                public void raiseFlag(char value) {
                    freg.set(value);
                }
            };
            mmu.registerExecutor(executor);

            mmu.execute(pop(reg()));
            assertTrue(executor.receivedInstructions.isEmpty());
            assertTrue(freg.isSet(FlagRegister.SEG_FLAG));

            // Should be 0, as no real executor is attached
            assertEquals(0, sp.getValue());

            freg.clear();
            sp.setValue((char) 1); // try with 1 as well (below required value)
            mmu.execute(pop(reg()));
            assertTrue(executor.receivedInstructions.isEmpty());
            assertTrue(freg.isSet(FlagRegister.SEG_FLAG));

            freg.clear();
            sp.setValue((char) 2); // 2 will succeed
            mmu.execute(pop(reg()));
            assertFalse(executor.receivedInstructions.isEmpty());
            assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        });
    }
}
